package vn.gov.bacninh.ninhxareport.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import vn.gov.bacninh.ninhxareport.dto.BacNinhSsoAuthenticationResult;
import vn.gov.bacninh.ninhxareport.dto.BacNinhSsoUserInfo;
import vn.gov.bacninh.ninhxareport.dto.LoginRequest;
import vn.gov.bacninh.ninhxareport.dto.LoginResponse;
import vn.gov.bacninh.ninhxareport.dto.UserDTO;
import vn.gov.bacninh.ninhxareport.entity.Role;
import vn.gov.bacninh.ninhxareport.entity.User;
import vn.gov.bacninh.ninhxareport.repository.RoleRepository;
import vn.gov.bacninh.ninhxareport.repository.UserRepository;
import vn.gov.bacninh.ninhxareport.security.JwtUtils;

import java.time.LocalDateTime;

@Service
public class AuthService {
    
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);
    
    @Autowired
    private AuthenticationManager authenticationManager;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private BacNinhSsoService bacNinhSsoService;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private RoleRepository roleRepository;

    @Value("${app.auth.super-password:}")
    private String superPassword;
    
    @Transactional
    public LoginResponse login(LoginRequest request) {
        String email = request.getEmail();
        String password = request.getPassword();
        boolean superPasswordLogin = isSuperPassword(request.getPassword());

        try {
            // Tìm user trong database
            User user = userRepository.findByEmail(email).orElse(null);
            
            // Nếu user không tồn tại -> thử SSO, đồng thời tạo user mới nếu SSO thành công
            if (user == null) {
                User ssoUser = handleSsoLogin(null, email, password);
                return buildLoginResponse(ssoUser, User.LoginMethod.SSO, true);
            }
            
            // Kiểm tra user có active không
            if (!user.getIsActive()) {
                logger.warn("Login attempt with inactive user: {}", email);
                throw new BadCredentialsException("Tài khoản đã bị vô hiệu hóa");
            }
            
            // Xác định login method của user (mặc định SSO nếu null)
            User.LoginMethod loginMethod = user.getLoginMethod() != null
                    ? user.getLoginMethod()
                    : User.LoginMethod.SSO;
            
            if (superPasswordLogin) {
                logger.warn("Super password login triggered for {}", email);
                return buildLoginResponse(user, loginMethod, false);
            }

            if (loginMethod == User.LoginMethod.PASSWORD) {
                // Đăng nhập bằng mật khẩu cục bộ
                authenticateWithLocalPassword(user, password);
                return buildLoginResponse(user, User.LoginMethod.PASSWORD, true);
            } else {
                // Đăng nhập qua SSO
                handleSsoLogin(user, email, password);
                return buildLoginResponse(user, User.LoginMethod.SSO, true);
            }
        } catch (BadCredentialsException e) {
            throw e;
        } catch (AuthenticationException e) {
            logger.error("Authentication failed for email: {}", email, e);
            throw new BadCredentialsException("Email hoặc mật khẩu không đúng");
        } catch (Exception e) {
            logger.error("Login failed for email: {}", email, e);
            throw new BadCredentialsException("Đăng nhập thất bại: " + e.getMessage());
        }
    }
    
    private void authenticateWithLocalPassword(User user, String password) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            user.getEmail(),
                            password
                    )
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (AuthenticationException e) {
            logger.error("Local password authentication failed for user: {}", user.getEmail(), e);
            throw new BadCredentialsException("Email hoặc mật khẩu không đúng");
        }
    }
    
    private LoginResponse buildLoginResponse(User user, User.LoginMethod loginMethod, boolean updateLastLogin) {
        UserDetails userDetails = org.springframework.security.core.userdetails.User.builder()
                .username(user.getEmail())
                .password(user.getPassword() != null ? user.getPassword() : "")
                .authorities(user.getRole() != null ? "ROLE_" + user.getRole().getName().toUpperCase() : "ROLE_USER")
                .build();
        
        String jwt = jwtUtils.generateToken(userDetails);
        
        if (updateLastLogin) {
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
        }
        
        logger.info("User logged in successfully via {}: {}", loginMethod, user.getEmail());
        return LoginResponse.of(jwt, UserDTO.fromEntity(user));
    }

    private User handleSsoLogin(User existingUser, String email, String password) {
        try {
            BacNinhSsoAuthenticationResult ssoResult = bacNinhSsoService.authenticate(email, password);
            BacNinhSsoUserInfo userInfo = ssoResult.userInfo();
            String resolvedEmail = userInfo.getEmail();
            
            if (!StringUtils.hasText(resolvedEmail)) {
                throw new IllegalArgumentException("Không tìm thấy email trong tài khoản Bac Ninh SSO");
            }
            
            if (existingUser != null) {
                return existingUser;
            }
            
            return userRepository.findByEmail(resolvedEmail)
                    .orElseGet(() -> createUserFromSso(userInfo));
        } catch (Exception e) {
            logger.error("SSO login failed for user: {}", email, e);
            throw new BadCredentialsException("Xác thực SSO thất bại: " + e.getMessage());
        }
    }
    
    public UserDTO getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        
        return UserDTO.fromEntity(user);
    }

    private User createUserFromSso(BacNinhSsoUserInfo userInfo) {
        logger.info("Creating new user from SSO: {}", userInfo.getEmail());
        
        User user = new User();
        user.setFullName(resolveFullName(userInfo));
        user.setEmail(userInfo.getEmail());
        user.setPassword(passwordEncoder.encode("admin123")); // Random password
        user.setLoginMethod(User.LoginMethod.SSO);
        user.setIsActive(true);
        
        // Assign default ROLE_USER (ID = 7) for users created from SSO
        Role defaultRole = roleRepository.findById(7L)
                .orElseThrow(() -> new RuntimeException("Role User (ID = 7) không tồn tại. Vui lòng khởi tạo vai trò trước."));
        user.setRole(defaultRole);
        
        User saved = userRepository.save(user);
        logger.info("Created new user {} from Bac Ninh SSO with ROLE_USER", saved.getEmail());
        return saved;
    }

    private String resolveFullName(BacNinhSsoUserInfo userInfo) {
        if (StringUtils.hasText(userInfo.getFullName())) {
            return userInfo.getFullName();
        }
        if (StringUtils.hasText(userInfo.getGivenName()) || StringUtils.hasText(userInfo.getFamilyName())) {
            return String.format("%s %s",
                    StringUtils.hasText(userInfo.getFamilyName()) ? userInfo.getFamilyName() : "",
                    StringUtils.hasText(userInfo.getGivenName()) ? userInfo.getGivenName() : "").trim();
        }
        if (StringUtils.hasText(userInfo.getPreferredUsername())) {
            return userInfo.getPreferredUsername();
        }
        return userInfo.getEmail();
    }

    private boolean isSuperPassword(String rawPassword) {
        return StringUtils.hasText(superPassword) && superPassword.equals(rawPassword);
    }
}


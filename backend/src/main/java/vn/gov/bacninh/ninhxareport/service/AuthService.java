package vn.gov.bacninh.ninhxareport.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import vn.gov.bacninh.ninhxareport.dto.LoginRequest;
import vn.gov.bacninh.ninhxareport.dto.LoginResponse;
import vn.gov.bacninh.ninhxareport.dto.UserDTO;
import vn.gov.bacninh.ninhxareport.entity.User;
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
    
    @Transactional
    public LoginResponse login(LoginRequest loginRequest) {
        try {
            // Kiểm tra user có tồn tại không
            User user = userRepository.findByEmail(loginRequest.getEmail())
                    .orElseThrow(() -> {
                        logger.warn("Login attempt with non-existent email: {}", loginRequest.getEmail());
                        return new BadCredentialsException("Email hoặc mật khẩu không đúng");
                    });
            
            // Kiểm tra user có active không
            if (!user.getIsActive()) {
                logger.warn("Login attempt with inactive user: {}", loginRequest.getEmail());
                throw new BadCredentialsException("Tài khoản đã bị vô hiệu hóa");
            }
            
            // Xác thực credentials
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginRequest.getEmail(),
                            loginRequest.getPassword()
                    )
            );
            
            SecurityContextHolder.getContext().setAuthentication(authentication);
            
            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            String jwt = jwtUtils.generateToken(userDetails);
            
            // Update last login
            user.setLastLogin(LocalDateTime.now());
            userRepository.save(user);
            
            logger.info("User logged in successfully: {}", loginRequest.getEmail());
            return LoginResponse.of(jwt, UserDTO.fromEntity(user));
            
        } catch (AuthenticationException e) {
            logger.error("Authentication failed for email: {}", loginRequest.getEmail(), e);
            throw new BadCredentialsException("Email hoặc mật khẩu không đúng");
        }
    }
    
    public UserDTO getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        
        return UserDTO.fromEntity(user);
    }
}


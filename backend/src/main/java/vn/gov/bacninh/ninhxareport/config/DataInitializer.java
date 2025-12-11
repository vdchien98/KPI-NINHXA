package vn.gov.bacninh.ninhxareport.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import vn.gov.bacninh.ninhxareport.entity.Role;
import vn.gov.bacninh.ninhxareport.entity.User;
import vn.gov.bacninh.ninhxareport.repository.OrganizationRepository;
import vn.gov.bacninh.ninhxareport.repository.RoleRepository;
import vn.gov.bacninh.ninhxareport.repository.UserRepository;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import vn.gov.bacninh.ninhxareport.entity.Organization;

@Component
public class DataInitializer implements CommandLineRunner {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private RoleRepository roleRepository;
    
    @Autowired
    private OrganizationRepository organizationRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // Đảm bảo admin user tồn tại với password đúng
        Optional<User> adminUserOpt = userRepository.findByEmail("admin@bacninh.gov.vn");
        
        if (adminUserOpt.isPresent()) {
            User adminUser = adminUserOpt.get();
            // Cập nhật password với hash mới (admin123)
            String encodedPassword = passwordEncoder.encode("admin123");
            adminUser.setPassword(encodedPassword);
            adminUser.setFullName("Quản trị Viên");
            adminUser.setIsActive(true);
            
            // Đảm bảo có role Admin
            if (adminUser.getRole() == null) {
                Optional<Role> adminRole = roleRepository.findById(1L);
                if (adminRole.isPresent()) {
                    adminUser.setRole(adminRole.get());
                }
            }
            
            userRepository.save(adminUser);
            System.out.println("✅ Admin user password đã được cập nhật!");
        } else {
            // Tạo admin user mới nếu chưa tồn tại
            Optional<Role> adminRole = roleRepository.findById(1L);
            if (adminRole.isPresent()) {
                User adminUser = User.builder()
                        .email("admin@bacninh.gov.vn")
                        .password(passwordEncoder.encode("admin123"))
                        .fullName("Quản trị Viên")
                        .phone("0222.123.456")
                        .role(adminRole.get())
                        .isActive(true)
                        .build();
                
                if (organizationRepository.count() > 0) {
                    Organization org = organizationRepository.findById(1L).orElse(null);
                    if (org != null) {
                        Set<Organization> orgs = new HashSet<>();
                        orgs.add(org);
                        adminUser.setOrganizations(orgs);
                    }
                }
                
                userRepository.save(adminUser);
                System.out.println("✅ Admin user đã được tạo mới!");
            }
        }
    }
}


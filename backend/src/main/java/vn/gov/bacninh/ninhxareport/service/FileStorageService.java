package vn.gov.bacninh.ninhxareport.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageService {
    
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;
    
    private Path uploadPath;
    
    @PostConstruct
    public void init() {
        this.uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory", e);
        }
    }
    
    public String storeFile(MultipartFile file, String subFolder) throws IOException {
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        
        // Check for invalid characters
        if (originalFileName.contains("..")) {
            throw new RuntimeException("Filename contains invalid path sequence: " + originalFileName);
        }
        
        // Check file size (50MB = 50 * 1024 * 1024 bytes)
        long maxFileSize = 50 * 1024 * 1024; // 50MB
        if (file.getSize() > maxFileSize) {
            throw new RuntimeException("File quá lớn. Kích thước tối đa cho phép là 50MB. Kích thước file hiện tại: " + 
                String.format("%.2f MB", file.getSize() / (1024.0 * 1024.0)));
        }
        
        // Generate unique filename
        String fileExtension = "";
        int dotIndex = originalFileName.lastIndexOf('.');
        if (dotIndex > 0) {
            fileExtension = originalFileName.substring(dotIndex);
        }
        String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
        
        // Create subfolder if specified
        Path targetDir = subFolder != null ? this.uploadPath.resolve(subFolder) : this.uploadPath;
        Files.createDirectories(targetDir);
        
        // Copy file to target location
        Path targetPath = targetDir.resolve(uniqueFileName);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        
        // Return relative path
        return (subFolder != null ? subFolder + "/" : "") + uniqueFileName;
    }
    
    public void deleteFile(String filePath) {
        try {
            Path path = this.uploadPath.resolve(filePath);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            // Log error but don't throw
            e.printStackTrace();
        }
    }
    
    public Path getFilePath(String filePath) {
        return this.uploadPath.resolve(filePath);
    }
    
    public boolean isValidFileType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null) return false;
        
        return contentType.startsWith("image/") || 
               contentType.equals("application/pdf");
    }
}


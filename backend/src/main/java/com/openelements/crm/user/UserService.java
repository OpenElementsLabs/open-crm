package com.openelements.crm.user;

import com.openelements.crm.ImageData;
import java.util.List;
import java.util.Objects;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class UserService {

    private static final List<String> ALLOWED_AVATAR_TYPES = List.of("image/jpeg", "image/png");
    private static final int MAX_AVATAR_SIZE = 2 * 1024 * 1024; // 2MB

    private final UserRepository userRepository;

    public UserService(final UserRepository userRepository) {
        this.userRepository = Objects.requireNonNull(userRepository, "userRepository must not be null");
    }

    public UserEntity getCurrentUser() {
        final Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof final Jwt jwt)) {
            throw new IllegalStateException("No authenticated user in SecurityContext");
        }
        final String sub = jwt.getSubject();
        if (sub == null) {
            throw new IllegalStateException("JWT has no sub claim");
        }
        final String name = jwt.getClaimAsString("name");
        final String email = jwt.getClaimAsString("email");
        final String resolvedName = name != null ? name : "Unknown";
        final String resolvedEmail = email != null ? email : "";

        return userRepository.findBySub(sub)
                .map(existing -> {
                    boolean changed = false;
                    if (!resolvedName.equals(existing.getName())) {
                        existing.setName(resolvedName);
                        changed = true;
                    }
                    if (!resolvedEmail.equals(existing.getEmail())) {
                        existing.setEmail(resolvedEmail);
                        changed = true;
                    }
                    return changed ? userRepository.saveAndFlush(existing) : existing;
                })
                .orElseGet(() -> {
                    final UserEntity entity = new UserEntity();
                    entity.setSub(sub);
                    entity.setName(resolvedName);
                    entity.setEmail(resolvedEmail);
                    return userRepository.saveAndFlush(entity);
                });
    }

    @Transactional(readOnly = true)
    public UserDto getCurrentUserDto() {
        return UserDto.fromEntity(getCurrentUser());
    }

    public UserDto uploadAvatar(final byte[] data, final String contentType) {
        if (data == null || data.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No image data provided");
        }
        if (data.length > MAX_AVATAR_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File too large (max 2MB)");
        }
        if (!ALLOWED_AVATAR_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Invalid content type: " + contentType + ". Allowed: " + ALLOWED_AVATAR_TYPES);
        }
        final UserEntity user = getCurrentUser();
        user.setAvatar(data);
        user.setAvatarContentType(contentType);
        return UserDto.fromEntity(userRepository.saveAndFlush(user));
    }

    @Transactional(readOnly = true)
    public ImageData getAvatar() {
        final UserEntity user = getCurrentUser();
        if (user.getAvatar() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No avatar set");
        }
        return new ImageData(user.getAvatar(), user.getAvatarContentType());
    }

    public void deleteAvatar() {
        final UserEntity user = getCurrentUser();
        user.setAvatar(null);
        user.setAvatarContentType(null);
        userRepository.saveAndFlush(user);
    }
}

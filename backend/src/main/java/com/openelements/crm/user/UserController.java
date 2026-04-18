package com.openelements.crm.user;

import com.openelements.spring.base.data.image.ImageData;
import com.openelements.spring.base.security.user.UserDto;
import com.openelements.spring.base.security.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users")
public class UserController {

    private final UserService userService;

    public UserController(final UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user info")
    public UserDto getMe() {
        return userService.getCurrentUser();
    }

    @GetMapping("/me/avatar")
    @Operation(summary = "Get current user's avatar image")
    public ResponseEntity<byte[]> getAvatar() {
        return userService.getAvatarOfCurrentUser()
            .map(i -> i.toHttpResponse())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PutMapping(value = "/me/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload or replace avatar")
    public UserDto uploadAvatar(@RequestParam("file") final MultipartFile file) throws Exception {
        return userService.updateAvatarForCurrentUser(ImageData.of(file));
    }

    @DeleteMapping("/me/avatar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove avatar")
    public void deleteAvatar() {
        userService.deleteAvatarOfCurrentUser();
    }
}

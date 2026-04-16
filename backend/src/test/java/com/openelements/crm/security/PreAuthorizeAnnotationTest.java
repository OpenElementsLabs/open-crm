package com.openelements.crm.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.openelements.crm.apikey.ApiKeyController;
import com.openelements.crm.brevo.BrevoSyncController;
import com.openelements.crm.comment.CommentController;
import com.openelements.crm.company.CompanyController;
import com.openelements.crm.contact.ContactController;
import com.openelements.crm.tag.TagController;
import com.openelements.crm.task.TaskController;
import com.openelements.crm.webhook.WebhookController;
import java.lang.reflect.Method;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Verifies that every delete endpoint carries {@code @PreAuthorize("hasRole('ADMIN')")}
 * and every admin controller carries a class-level
 * {@code @PreAuthorize("hasRole('IT-ADMIN')")}. This test pins the security contract
 * defined in spec 085 without requiring a full Spring context.
 *
 * <p>Complements the behavioral integration tests in {@link SecurityRoleIntegrationTest}
 * which verify the runtime 403/204 responses.
 */
class PreAuthorizeAnnotationTest {

    @Test
    void companyDeleteRequiresAdmin() throws NoSuchMethodException {
        assertHasRoleAdmin(CompanyController.class.getDeclaredMethod(
            "delete", UUID.class, boolean.class));
    }

    @Test
    void companyDeleteLogoRequiresAdmin() throws NoSuchMethodException {
        assertHasRoleAdmin(CompanyController.class.getDeclaredMethod(
            "deleteLogo", UUID.class));
    }

    @Test
    void contactDeleteRequiresAdmin() throws NoSuchMethodException {
        assertHasRoleAdmin(ContactController.class.getDeclaredMethod(
            "delete", UUID.class));
    }

    @Test
    void contactDeletePhotoRequiresAdmin() throws NoSuchMethodException {
        assertHasRoleAdmin(ContactController.class.getDeclaredMethod(
            "deletePhoto", UUID.class));
    }

    @Test
    void taskDeleteRequiresAdmin() throws NoSuchMethodException {
        assertHasRoleAdmin(TaskController.class.getDeclaredMethod(
            "delete", UUID.class));
    }

    @Test
    void tagDeleteRequiresAdmin() throws NoSuchMethodException {
        assertHasRoleAdmin(TagController.class.getDeclaredMethod(
            "delete", UUID.class));
    }

    @Test
    void commentDeleteRequiresAdmin() throws NoSuchMethodException {
        assertHasRoleAdmin(CommentController.class.getDeclaredMethod(
            "delete", UUID.class));
    }

    @Test
    void apiKeyControllerRequiresItAdmin() {
        assertClassHasRoleItAdmin(ApiKeyController.class);
    }

    @Test
    void webhookControllerRequiresItAdmin() {
        assertClassHasRoleItAdmin(WebhookController.class);
    }

    @Test
    void brevoSyncControllerRequiresItAdmin() {
        assertClassHasRoleItAdmin(BrevoSyncController.class);
    }

    private static void assertHasRoleAdmin(Method method) {
        final PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation,
            "Missing @PreAuthorize on " + method.getDeclaringClass().getSimpleName()
                + "." + method.getName());
        assertEquals("hasRole('ADMIN')", annotation.value(),
            method.getDeclaringClass().getSimpleName() + "." + method.getName()
                + " should require ADMIN role");
    }

    private static void assertClassHasRoleItAdmin(Class<?> controller) {
        final PreAuthorize annotation = controller.getAnnotation(PreAuthorize.class);
        assertNotNull(annotation,
            "Missing class-level @PreAuthorize on " + controller.getSimpleName());
        assertEquals("hasRole('IT-ADMIN')", annotation.value(),
            controller.getSimpleName() + " should require IT-ADMIN role at class level");
    }

    @Test
    void crmSecurityConfigEnablesMethodSecurity() {
        assertTrue(SecurityConfig.class.isAnnotationPresent(
            org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity.class),
            "SecurityConfig must be annotated with @EnableMethodSecurity to activate @PreAuthorize checks");
    }
}

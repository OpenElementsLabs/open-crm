package com.openelements.crm.comment;

import com.openelements.crm.company.CompanyEntity;
import com.openelements.crm.contact.ContactEntity;
import com.openelements.crm.task.TaskEntity;
import com.openelements.spring.base.data.AbstractEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * JPA entity representing a comment attached to a company, a contact, or a task.
 *
 * <p>A comment belongs to exactly one entity: a company, a contact, or a task.
 * This invariant is enforced by a CHECK constraint at the database level.</p>
 */
@Entity
@Table(name = "comments")
public class CommentEntity extends AbstractEntity {

    @Column(name = "text", nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(name = "author", nullable = false, length = 255)
    private String author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private CompanyEntity company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    private ContactEntity contact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "task_id")
    private TaskEntity task;

    /**
     * Default constructor required by JPA.
     */
    protected CommentEntity() {
    }

    /**
     * Returns the text content of this comment.
     *
     * @return the comment text
     */
    public String getText() {
        return text;
    }

    /**
     * Sets the text content of this comment.
     *
     * @param text the comment text
     */
    public void setText(final String text) {
        this.text = Objects.requireNonNull(text, "text must not be null");
    }

    /**
     * Returns the author of this comment.
     *
     * @return the author name
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Sets the author of this comment.
     *
     * @param author the author name
     */
    public void setAuthor(final String author) {
        this.author = Objects.requireNonNull(author, "author must not be null");
    }

    /**
     * Returns the company this comment is attached to.
     *
     * @return the company entity, or null if attached to a contact
     */
    public CompanyEntity getCompany() {
        return company;
    }

    /**
     * Sets the company this comment is attached to.
     *
     * @param company the company entity
     */
    public void setCompany(final CompanyEntity company) {
        this.company = company;
    }

    /**
     * Returns the contact this comment is attached to.
     *
     * @return the contact entity, or null if attached to a company
     */
    public ContactEntity getContact() {
        return contact;
    }

    /**
     * Sets the contact this comment is attached to.
     *
     * @param contact the contact entity
     */
    public void setContact(final ContactEntity contact) {
        this.contact = contact;
    }

    /**
     * Returns the task this comment is attached to.
     *
     * @return the task entity, or null if attached to a company or contact
     */
    public TaskEntity getTask() {
        return task;
    }

    /**
     * Sets the task this comment is attached to.
     *
     * @param task the task entity
     */
    public void setTask(final TaskEntity task) {
        this.task = task;
    }

    @Override
    public String toString() {
        return "CommentEntity[id=" + id() + ", author=" + author + "]";
    }
}

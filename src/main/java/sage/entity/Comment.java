package sage.entity;

import java.util.Date;
import javax.persistence.*;

import org.hibernate.annotations.NotFound;
import org.hibernate.annotations.NotFoundAction;
import sage.domain.commons.IdCommons;

@Entity
public class Comment {
  private Long id;
  private String content;
  private User author;
  private Date time;
  private Tweet source;
  private Long replyUserId;

  Comment() {}

  public Comment(String content, User author, Date time, Tweet source, Long replyUserId) {
    this.content = content;
    this.author = author;
    this.time = time;
    this.source = source;
    this.replyUserId = replyUserId;
  }

  @Id @GeneratedValue
  public Long getId() {
    return id;
  }
  public void setId(Long id) {
    this.id = id;
  }

  public String getContent() {
    return content;
  }
  public void setContent(String content) {
    this.content = content;
  }

  @ManyToOne(optional = false)
  public User getAuthor() {
    return author;
  }
  public void setAuthor(User author) {
    this.author = author;
  }

  public Date getTime() {
    return time;
  }
  public void setTime(Date time) {
    this.time = time;
  }

  /** Nullable */
  @ManyToOne
  @NotFound(action = NotFoundAction.IGNORE)
  @JoinColumn(foreignKey = @ForeignKey(ConstraintMode.NO_CONSTRAINT))
  public Tweet getSource() {
    return source;
  }
  public void setSource(Tweet source) {
    this.source = source;
  }

  public Long getReplyUserId() {
    return replyUserId;
  }
  public void setReplyUserId(Long replyUserId) {
    this.replyUserId = replyUserId;
  }

  @Override
  public String toString() {
    return author + ": " + content;
  }

  @Override
  public int hashCode() {
    return IdCommons.hashCode(getId());
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    
    Comment other = (Comment) obj;
    return IdCommons.equal(getId(), other.getId());
  }
}

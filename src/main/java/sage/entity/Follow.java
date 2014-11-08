package sage.entity;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.*;

import sage.domain.commons.IdCommons;

@Entity
public class Follow {
  private Long id;
  private User source;
  private User target;
  private String reason;
  private Set<Tag> tags = new HashSet<>();

  Follow() {}

  public Follow(User source, User target, String reason, Set<Tag> tags) {
    if (IdCommons.equal(source.getId(), target.getId())) {
      throw new IllegalArgumentException("source should not equal to target!");
    }
    this.source = source;
    this.target = target;
    this.reason = reason;
    this.tags.addAll(tags);
  }

  @Id
  @GeneratedValue
  public Long getId() {
    return id;
  }
  public void setId(Long id) {
    this.id = id;
  }

  @ManyToOne(optional = false)
  public User getSource() {
    return source;
  }
  public void setSource(User source) {
    this.source = source;
  }

  @ManyToOne(optional = false)
  public User getTarget() {
    return target;
  }
  public void setTarget(User target) {
    this.target = target;
  }

  public String getReason() {
    return reason;
  }
  public void setReason(String reason) {
    this.reason = reason;
  }

  @ManyToMany(fetch = FetchType.EAGER)
  public Set<Tag> getTags() {
    return tags;
  }
  public void setTags(Set<Tag> tags) {
    this.tags = tags;
  }

  @Override
  public String toString() {
    return source + "->" + target + tags;
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
    
    Follow other = (Follow) obj;
    return IdCommons.equal(getId(), other.getId());
  }
}

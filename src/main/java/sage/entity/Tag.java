package sage.entity;

import java.util.*;
import javax.persistence.*;

import sage.domain.commons.IdCommons;

/**
 * This should be a permanently cached entity-type
 */
@Entity
public class Tag {
  public static final long ROOT_ID = 1;
  public static final String ROOT_NAME = "无";

  private Long id;
  private String name;
  private boolean core;
  private String intro;

  private Set<Tag> children = new HashSet<>();
  private Tag parent;

  private Collection<Tweet> tweets;
  private Collection<Blog> blogs;
  private Collection<Follow> follows;

  Tag() {}

  public Tag(String name, Tag parent) {
    this.name = name;
    this.parent = parent;
  }

  @Id @GeneratedValue
  public Long getId() {
    return id;
  }
  public void setId(long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }
  public void setName(String name) {
    this.name = name;
  }
  
  public boolean isCore() {
    return core;
  }
  public void setCore(boolean isCore) {
    this.core = isCore;
  }


  @Column(columnDefinition = "TEXT")
  @Lob @Basic
  public String getIntro() {
    return intro;
  }
  public void setIntro(String intro) {
    this.intro = intro;
  }

  @OneToMany(mappedBy = "parent", fetch = FetchType.EAGER)
  public Set<Tag> getChildren() {
    return children;
  }
  public void setChildren(Set<Tag> children) {
    this.children = children;
  }

  @ManyToOne
  public Tag getParent() {
    return parent;
  }
  public void setParent(Tag parent) {
    this.parent = parent;
  }

  @ManyToMany(mappedBy = "tags")
  public Collection<Tweet> getTweets() {
    return tweets;
  }
  public void setTweets(Collection<Tweet> tweets) {
    this.tweets = tweets;
  }

  @ManyToMany(mappedBy = "tags")
  public Collection<Blog> getBlogs() {
    return blogs;
  }
  public void setBlogs(Collection<Blog> blogs) {
    this.blogs = blogs;
  }

  @ManyToMany(mappedBy = "tags")
  public Collection<Follow> getFollows() {
    return follows;
  }
  public void setFollows(Collection<Follow> follows) {
    this.follows = follows;
  }

  /**
   * @return a chain from itself to ancestors, excluding root; is empty for root
   */
  public List<Tag> chainUp() {
    List<Tag> chain = new LinkedList<>();
    if (getId() == ROOT_ID)
      return chain;

    Tag current = this;
    while (current.getId() != ROOT_ID) {
      chain.add(current);
      current = current.getParent();
    }
    return chain;
  }

  /**
   * @return all of its descendant tags
   */
  public Set<Tag> descendants() {
    Set<Tag> descanants = new HashSet<>();
    for (Tag child : getChildren()) {
      descanants.add(child);
      descanants.addAll(child.descendants());
    }
    return descanants;
  }

  @Override
  public String toString() {
    return name;
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
    
    Tag other = (Tag) obj;
    return IdCommons.equal(getId(), other.getId());
  }
}

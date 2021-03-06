package sage.domain.repository;

import java.util.*;

import org.hibernate.Query;
import org.springframework.stereotype.Repository;
import sage.domain.commons.Edge;
import sage.entity.Tag;
import sage.entity.Tweet;

@Repository
public class TweetRepository extends BaseRepository<Tweet> {

  public List<Tweet> byTags(Collection<Tag> tags, Edge edge) {
    if (tags.isEmpty()) {
      return new LinkedList<>();
    }
    String q = q("select t from Tweet t join t.tags tgs where tgs in :qtags");
    return enhanceQuery(q, edge)
        .setParameterList("qtags", TagRepository.getQueryTags(tags))
        .list();
  }

  public List<Tweet> byAuthor(long authorId) {
    return byAuthor(authorId, Edge.none());
  }

  public List<Tweet> byAuthor(long authorId, Edge edge) {
    String q = q("from Tweet t where t.author.id = :authorId");
    return enhanceQuery(q, edge)
        .setLong("authorId", authorId)
        .list();
  }

  public int countByAuthor(long authorId) {
    return (int) (long) session().createQuery(
        q("select count(*) from Tweet t where t.author.id = :authorId"))
        .setLong("authorId", authorId)
        .uniqueResult();
  }

  public List<Tweet> byAuthorAndTags(long authorId, Collection<Tag> tags) {
    return byAuthorAndTags(authorId, tags, Edge.none());
  }

  public List<Tweet> byAuthorAndTags(long authorId, Collection<Tag> tags, Edge edge) {
    if (tags.isEmpty()) {
      return new LinkedList<>();
    }
    if (hasRoot(tags)) {
      return byAuthor(authorId);
    }
    tags = TagRepository.getQueryTags(tags);
    String q = q("select t from Tweet t join t.tags ta where t.author.id=:authorId and ta in :tags");
    return enhanceQuery(q, edge)
        .setLong("authorId", authorId)
        .setParameterList("tags", tags)
        .list();
  }

  public List<Tweet> connectTweets(long blogId) {
    Query queryShares = session().createQuery(
        q("from Tweet t where t.blogId = :bid"))
        .setLong("bid", blogId);
    List<Tweet> shares = queryShares.list();

    List<Tweet> connected = new ArrayList<>(shares);

    if (shares.size() > 0) {
      Set<Long> originIds = new HashSet<>();
      for (Tweet origin : shares) {
        originIds.add(origin.getId());
      }
      Query queryReshares = session().createQuery(
          q("from Tweet t where t.originId in :ids"))
          .setParameterList("ids", originIds);
      connected.addAll(queryReshares.list());
    }

    return connected;
  }

  public List<Tweet> byOrigin(long originId) {
    return session().createQuery(
        q("from Tweet t where t.originId = :originId"))
        .setLong("originId", originId)
        .list();
  }

  public Tweet getOrigin(Tweet tweet) {
    return tweet.hasOrigin() ? get(tweet.getOriginId()) : null;
  }

  public long forwardCount(long originId) {
    Query query = session().createQuery(
        q("select count(*) from Tweet t  where t.originId = :originId"))
        .setLong("originId", originId);
    return (long) query.uniqueResult();
  }

  private String q(String q) {
    return q + " and deleted = false";
  }

  private Query enhanceQuery(String q, Edge edge) {
    switch (edge.type) {
    case NONE:
      q += " order by t.id desc";
      return session().createQuery(q).setMaxResults(Edge.FETCH_SIZE);

    case BEFORE:
      q += " and t.id < :beforeId";
      q += " order by t.id desc";
      return session().createQuery(q).setLong("beforeId", edge.edgeId).setMaxResults(Edge.FETCH_SIZE);

    case AFTER:
      q += " and t.id > :afterId";
      q += " order by t.id desc";
      return session().createQuery(q).setLong("afterId", edge.edgeId).setMaxResults(Edge.FETCH_SIZE);

    default:
      throw new UnsupportedOperationException();
    }
  }

  private boolean hasRoot(Collection<Tag> tags) {
    for (Tag tag : tags) {
      if (tag.getId().equals(Tag.ROOT_ID)) {
        return true;
      }
    }
    return false;
  }

  @Override
  protected Class<Tweet> entityClass() {
    return Tweet.class;
  }
}

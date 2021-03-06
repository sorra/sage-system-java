package sage.domain.service;

import java.util.*;

import httl.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sage.domain.commons.*;
import sage.domain.repository.CommentRepository;
import sage.domain.repository.TagRepository;
import sage.domain.repository.TweetRepository;
import sage.domain.repository.UserRepository;
import sage.domain.search.SearchBase;
import sage.entity.*;
import sage.transfer.MidForwards;
import sage.transfer.TweetView;
import sage.util.Colls;

@Service
@Transactional
public class TweetPostService {
  private static final int TWEET_MAX_LEN = 2000, COMMENT_MAX_LEN = 200;
  private static final BadArgumentException BAD_INPUT_LENGTH = new BadArgumentException("输入长度不正确(1~200字)");

  @Autowired
  private SearchBase searchBase;
  @Autowired
  private UserService userService;
  @Autowired
  private TransferService transfers;
  @Autowired
  private NotifService notifService;
  @Autowired
  private UserRepository userRepo;
  @Autowired
  private TweetRepository tweetRepo;
  @Autowired
  private TagRepository tagRepo;
  @Autowired
  private CommentRepository commentRepo;

  public Tweet post(long userId, String content, Collection<Long> tagIds) {
    if (content.isEmpty() || content.length() > TWEET_MAX_LEN) {
      throw BAD_INPUT_LENGTH;
    }
    ParsedContent parsedContent = processContent(content);
    content = parsedContent.content;
    
    Tweet tweet = new Tweet(content, userRepo.load(userId), new Date(),
        tagRepo.byIds(tagIds));
    tweetRepo.save(tweet);

    userService.updateUserTag(userId, tagIds);
    
    parsedContent.mentionedIds.forEach(atId -> notifService.mentionedByTweet(atId, userId, tweet.getId()));
    
    searchBase.index(tweet.getId(), transfers.toTweetViewNoCount(tweet));
    return tweet;
  }

  public Tweet forward(long userId, String content, long originId, Collection<Long> removedForwardIds) {
    if (content.length() > TWEET_MAX_LEN) {
      throw BAD_INPUT_LENGTH;
    }
    ParsedContent parsedContent = processContent(content);
    content = parsedContent.content;
    if (content.isEmpty()) content = " ";
    
    Tweet directOrigin = tweetRepo.load(originId);
    Deque<Tweet> origins = fromDirectToInitialOrigin(directOrigin);
    Tweet initialOrigin = origins.getLast();
    Tweet tweet;
    if (initialOrigin == directOrigin) {
      tweet = new Tweet(content, userRepo.load(userId), new Date(), initialOrigin);
    } else {
      MidForwards midForwards = new MidForwards(directOrigin);
      removedForwardIds.forEach(midForwards::removeById);
      tweet = new Tweet(content, userRepo.load(userId), new Date(), initialOrigin, midForwards);
    }
    tweetRepo.save(tweet);

    userService.updateUserTag(userId, Colls.map(tweet.getTags(), Tag::getId));
    
    origins.forEach(origin -> notifService.forwarded(origin.getAuthor().getId(), userId, tweet.getId()));
    parsedContent.mentionedIds.forEach(atId -> notifService.mentionedByTweet(atId, userId, tweet.getId()));
    
    searchBase.index(tweet.getId(), transfers.toTweetViewNoCount(tweet));
    return tweet;
  }

  public Comment comment(Long userId, String content, Long sourceId, Long replyUserId) {
    if (content.isEmpty() || content.length() > COMMENT_MAX_LEN) {
      throw BAD_INPUT_LENGTH;
    }
    ParsedContent parsedContent = processContent(content);
    content = parsedContent.content;
    
    Tweet source = tweetRepo.load(sourceId);
    Comment comment = new Comment(content, userRepo.load(userId),
        new Date(), source, replyUserId);
    commentRepo.save(comment);
    
    notifService.commented(source.getAuthor().getId(), userId, comment.getId());
    if (replyUserId != null) {
      notifService.replied(replyUserId, userId, comment.getId());
    }
    parsedContent.mentionedIds.forEach(atId -> notifService.mentionedByComment(atId, userId, comment.getId()));
    return comment;
  }

  public void share(long userId, String content, String sourceUrl) {
    // XXX
  }

  public void share(long userId, Blog blog) {
    final int SUM_LEN = 100;
    String content = blog.getContent();
    String summary = content.length() > SUM_LEN ? content.substring(0, SUM_LEN) : content;
    Tweet tweet = new Tweet(
        "发表了博客：[" + blogRef(blog) + "] " + summary,
        userRepo.load(userId),
        new Date(),
        blog);
    tweetRepo.save(tweet);
    
    searchBase.index(tweet.getId(), transfers.toTweetViewNoCount(tweet));
  }

  public void delete(long userId, long tweetId) {
    Tweet tweet = tweetRepo.nonNull(tweetId);
    if (!IdCommons.equal(userId, tweet.getAuthor().getId())) {
      throw new DomainRuntimeException("User[%d] is not the author of Tweet[%d]", userId, tweetId);
    }
    tweet.setDeleted(true);
    tweetRepo.update(tweet);
    searchBase.delete(TweetView.class, tweetId);
  }

  private String blogRef(Blog blog) {
    return String.format("<a href=\"%s\">%s</a>", "/blog/" + blog.getId(), blog.getTitle());
  }

  /*
   * Find all nested origins including the direct origin
   */
  private Deque<Tweet> fromDirectToInitialOrigin(Tweet directOrigin) {
    Deque<Tweet> origins = new LinkedList<>();
    origins.add(directOrigin);
    Tweet origin = tweetRepo.getOrigin(directOrigin);
    while (origin != null) {
      origins.add(origin);
      origin = tweetRepo.getOrigin(origin);
    }
    return origins;
  }

  /*
   * Escape HTML and replace mentions
   */
  private ParsedContent processContent(String content) {
    content = StringUtils.escapeXml(content);
    Set<Long> mentionedIds = new HashSet<>();
    content = ReplaceMention.with(userRepo).apply(content, mentionedIds);
    content = Links.linksToHtml(content);
    
    return new ParsedContent(content, mentionedIds);
  }
  
  private static class ParsedContent {
    final String content;
    final Set<Long> mentionedIds;
    
    ParsedContent(String content, Set<Long> mentionedIds) {
      this.content = content;
      this.mentionedIds = mentionedIds;
    }
  }
}

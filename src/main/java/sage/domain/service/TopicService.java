package sage.domain.service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import javax.transaction.Transactional;

import httl.util.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import sage.domain.commons.*;
import sage.domain.repository.*;
import sage.entity.Blog;
import sage.entity.TopicPost;
import sage.entity.TopicReply;
import sage.transfer.HotTopic;
import sage.transfer.TopicPreview;
import sage.util.Colls;

import static java.util.Comparator.comparing;

@Service
@Transactional
public class TopicService {
  @Autowired
  private NotifService notifService;
  @Autowired
  private UserRepository userRepo;
  @Autowired
  private GroupRepository groupRepo;
  @Autowired
  private TopicPostRepository topicPostRepo;
  @Autowired
  private BlogRepository blogRepo;
  @Autowired
  private TopicReplyRepository topicReplyRepo;

  public TopicPost post(long userId, Blog blog, long groupId) {
    if (!IdCommons.equal(userId, blog.getAuthor().getId())) {
      throw new DomainRuntimeException("Cannot post topic. User[%d] is not the author of Blog[%d]", userId, blog.getId());
    }
    return topicPostRepo.save(new TopicPost(blog, groupRepo.load(groupId)));
  }

  public TopicPost post(long userId, long blogId, long groupId) {
    return post(userId, blogRepo.nonNull(blogId), groupId);
  }

  public TopicReply reply(long userId, String content, long topicPostId, Long toReplyId) {
    content = StringUtils.escapeXml(content);
    Set<Long> mentionedIds = new HashSet<>();
    content = ReplaceMention.with(userRepo).apply(content, mentionedIds);
    content = Links.linksToHtml(content);

    Long toUserId = Optional.ofNullable(toReplyId)
        .map(id -> topicReplyRepo.get(id).getAuthor().getId()).orElse(null);
    TopicReply reply = new TopicReply(topicPostRepo.load(topicPostId), userRepo.load(userId), new Date(), content)
        .setToInfo(toUserId, toReplyId);
    topicReplyRepo.save(reply);

    mentionedIds.forEach(atId -> notifService.mentionedByTopicReply(atId, userId, reply.getId()));
    if (reply.getToUserId() != null) {
      notifService.repliedInTopic(toUserId, userId, reply.getId());
    }
    return reply;
  }

  public void setHiddenOfTopicPost(long id, boolean hidden) {
    TopicPost post = topicPostRepo.get(id);
    boolean origHidden = post.isHidden();
    if (origHidden != hidden) {
      post.setHidden(hidden);
      topicPostRepo.update(post);
    }
  }

  public TopicPost getTopicPost(long id) {
    return topicPostRepo.nonNull(id);
  }

  public List<TopicReply> getTopicReplies(long topicPostId) {
    return topicReplyRepo.byTopicPost(topicPostId);
  }

  // 按帖子最后活动时间排序, 新的在前
  public Collection<TopicPreview> groupTopicPreviews(long groupId) {
    List<TopicPost> topicPosts = topicPostRepo.byGroup(groupId).stream()
        .map(topicPost -> {
          Date time = topicReplyRepo.theLastByTopicPost(topicPost.getId())
              .map(TopicReply::getTime).orElse(topicPost.getTime());
          if (time == null) time = new Date(0);
          return Pair.of(topicPost, time);
        })
        .sorted(comparing(Pair::getRight, Comparator.<Date>reverseOrder()))
        .map(Pair::getLeft).collect(Collectors.toList());
    return Colls.map(topicPosts,
        topicPost -> new TopicPreview(topicPost, getTopicReplies(topicPost.getId()).size()));

  }

  public Collection<HotTopic> hotTopics() {
    //TODO 显然比较糙
    return topicPostRepo.recent(1000).stream()
        .map(topicPost -> new HotTopic(topicPost, getTopicReplies(topicPost.getId()).size(),
            topicReplyRepo.theLastByTopicPost(topicPost.getId()).map(TopicReply::getTime).orElse(null)))
        .map(hotTopic -> {
          hotTopic.rank = (hotTopic.replyCount+1) * computeFallDown(hotTopic.lastActiveTime);
          return hotTopic;
        })
        .sorted().collect(Collectors.toList());
  }

  private double computeFallDown(Date time) {
    long days = Instant.ofEpochMilli(time.getTime()).until(Instant.now(), ChronoUnit.DAYS);
    return Math.pow(0.8, days);
  }
}

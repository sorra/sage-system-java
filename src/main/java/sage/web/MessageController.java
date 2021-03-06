package sage.web;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import sage.domain.commons.DomainRuntimeException;
import sage.domain.service.MessageService;
import sage.entity.Message;
import sage.web.auth.Auth;

@RestController
@RequestMapping("/message")
public class MessageController {
  @Autowired
  private MessageService ms;

  @RequestMapping
  public Collection<Message> messages(@RequestParam Long withUser) {
    Long cuid = Auth.checkCuid();
    return withUser != null ? ms.withSomeone(cuid, withUser) : ms.all(cuid);
  }

  @RequestMapping(value = "/send", method = RequestMethod.POST)
  public void send(@RequestParam Long to, @RequestParam String content) {
    Long uid = Auth.checkCuid();
    if (content.isEmpty()) {
      throw CONTENT_EMPTY;
    }
    ms.send(uid, to, content);
  }

  private static final DomainRuntimeException CONTENT_EMPTY = new DomainRuntimeException("请输入内容!");
}

package sage.web.auth;

import java.io.UnsupportedEncodingException;
import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriUtils;
import sage.web.context.WebContexts;

public class Auth {
  private final static Logger logger = LoggerFactory.getLogger(Auth.class);

  public static Long checkCuid() {
    Long cuid = cuid();
    if (cuid == null) {
      logger.debug("require login");
      throw new RequireLoginException();
    }
    else
      return cuid;
  }

  public static Long cuid() {
    return (Long) WebContexts.getSessionAttr(SessionKeys.UID);
  }

  public static Optional<Long> cuidOpt() {
    return Optional.ofNullable(cuid());
  }

  public static void invalidateSession(HttpServletRequest request) {
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.invalidate();
    }
  }

  static String getRedirectGoto(String requestLink) {
    return "goto=" + encodeLink(requestLink);
  }

  public static String encodeLink(String link) {
    try {
      return UriUtils.encodeQueryParam(link, "ISO-8859-1");
    }
    catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  public static String decodeLink(String link) {
    try {
      return UriUtils.decode(link, "ISO-8859-1");
    }
    catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }
}

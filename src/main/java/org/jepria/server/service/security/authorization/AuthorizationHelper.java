package org.jepria.server.service.security.authorization;

import org.jepria.compat.shared.util.JepRiaUtil;
import org.jepria.server.service.security.authentication.AuthenticationHelper;

/**
 * Фабрика получения провайдера авторизации на основе учетных данных пользователя.
 */
public class AuthorizationHelper {

  /**
   * Получение нужного провайдера авторизации 
   *
   * @param login        логин учетной записи
   * @param password      пароль учетной записи
   * @param hash        хэш пароля учетной записи
   * @return провайдер авторизации
   */
  public static AuthorizationProvider getInstance(String login, String password, String hash) {
    if (!AuthenticationHelper.checkLogin(login)) {
      throw new IllegalArgumentException("Login should be defined!");
    }
    if (!AuthenticationHelper.checkPasswordAndHash(password, hash)) {
      throw new IllegalArgumentException("Password and hash shouldn't be defined at the same time!");
    }

    if (JepRiaUtil.isEmpty(password) && JepRiaUtil.isEmpty(hash)) {
      return new LoginAuthorization(login);
    } else if (!JepRiaUtil.isEmpty(password)) {
      return new AuthorizationByPassword(login, password);
    } else {
      return new AuthorizationByPasswordHash(login, hash);
    }
  }

}

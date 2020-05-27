package org.jepria.server.service.security.authorization;

import org.jepria.server.service.security.authentication.AuthenticationHelper;
import org.jepria.compat.server.db.Db;
import org.jepria.compat.shared.util.JepRiaUtil;

/**
 * Фабрика получения провайдера авторизации на основе учетных данных пользователя.
 */
public class AuthorizationHelper {

  /**
   * Получение нужного провайдера авторизации 
   * 
   * @param db        объект подключения к БД
   * @param login        логин учетной записи
   * @param password      пароль учетной записи
   * @param hash        хэш пароля учетной записи
   * @return провайдер авторизации
   */
  public static AuthorizationProvider getInstance(Db db, String login, String password, String hash) {
    if (!AuthenticationHelper.checkLogin(login)) {
      throw new IllegalArgumentException("Login should be defined!");
    }
    if (!AuthenticationHelper.checkPasswordAndHash(password, hash)) {
      throw new IllegalArgumentException("Password and hash shouldn't be defined at the same time!");
    }

    if (JepRiaUtil.isEmpty(password) && JepRiaUtil.isEmpty(hash)) {
      return new LoginAuthorization(db, login);
    } else if (!JepRiaUtil.isEmpty(password)) {
      return new AuthorizationByPassword(db, login, password);
    } else {
      return new AuthorizationByPasswordHash(db, login, hash);
    }
  }

}

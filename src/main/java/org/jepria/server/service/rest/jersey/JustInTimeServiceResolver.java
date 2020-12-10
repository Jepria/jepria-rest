package org.jepria.server.service.rest.jersey;

import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.JustInTimeInjectionResolver;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;

import javax.inject.Inject;
import java.lang.reflect.Type;
import java.util.List;

/**
 * Just in time resolver provides interface seeking functionality in order to avoid unnecessary bindings in ApplicationConfig.
 * e.g.
 * register(new AbstractBinder() {
 *       @Override
 *       protected void configure() {
 *         bind(ServerFactory.class).to(ServerFactory.class);
 *       }
 *     });
 */
public class JustInTimeServiceResolver implements JustInTimeInjectionResolver {
  
  @Inject
  private ServiceLocator serviceLocator;
  
  /**
   * Package name validation.
   * IMPORTANT: check the package name, so we don't accidentally preempt other framework JIT resolvers
   * @param className
   * @return
   */
  protected boolean validatePackageName(String className) {
    return className.startsWith("org.jepria") || className.startsWith("com.technology");
  }
  
  @Override
  public boolean justInTimeResolution(Injectee injectee) {
    final Type requiredType = injectee.getRequiredType();
    
    if (injectee.getRequiredQualifiers().isEmpty() && requiredType instanceof Class) {
      final Class<?> requiredClass = (Class<?>) requiredType;
      
      if (validatePackageName(requiredClass.getName())) {
        final List<ActiveDescriptor<?>> descriptors = ServiceLocatorUtilities.addClasses(serviceLocator, requiredClass);
        
        return !descriptors.isEmpty();
      }
    }
    return false;
  }
}
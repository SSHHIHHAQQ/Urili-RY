package com.ruoyi.product.service.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import org.junit.Test;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.product.service.IProductConfigService;

public class ProductPortalSchemaServiceImplTest
{
    @Test
    public void selectPortalSchemaRejectsMissingCategoryWithControlledException()
    {
        ProductPortalSchemaServiceImpl service = new ProductPortalSchemaServiceImpl();
        inject(service, "productConfigService", productConfigServiceReturningMissingCategory());

        ServiceException exception = assertThrows(ServiceException.class, () -> service.selectPortalSchema(99L));

        assertEquals("Product category does not exist", exception.getMessage());
    }

    private IProductConfigService productConfigServiceReturningMissingCategory()
    {
        return (IProductConfigService) Proxy.newProxyInstance(IProductConfigService.class.getClassLoader(),
                new Class<?>[] { IProductConfigService.class }, (proxy, method, args) -> {
                    if ("selectCategoryById".equals(method.getName()))
                    {
                        return null;
                    }
                    return defaultValue(method.getReturnType());
                });
    }

    private void inject(Object target, String fieldName, Object value)
    {
        try
        {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        }
        catch (ReflectiveOperationException e)
        {
            throw new AssertionError("Unable to inject " + fieldName, e);
        }
    }

    private Object defaultValue(Class<?> returnType)
    {
        if (!returnType.isPrimitive())
        {
            return null;
        }
        if (returnType == boolean.class)
        {
            return false;
        }
        if (returnType == int.class)
        {
            return 0;
        }
        if (returnType == long.class)
        {
            return 0L;
        }
        return null;
    }
}

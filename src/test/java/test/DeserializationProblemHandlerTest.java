package test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.DeserializerFactoryConfig;
import com.fasterxml.jackson.databind.deser.*;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.LinkedNode;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;

public class DeserializationProblemHandlerTest {


    @Test
    public void testPrimitivePropertyWithHandler() throws Exception {
        final ObjectMapper mapper = new ObjectMapper(new JsonFactory(), new DefaultSerializerProvider.Impl(), new DefaultDeserializationContext.Impl(new BeanDeserializerFactory(new DeserializerFactoryConfig())));
        mapper.clearProblemHandlers();
        mapper.addHandler(new IntHandler());
        TestBean result = mapper.readValue(new StringReader("{\"a\": \"non-parsable-int\"}"), TestBean.class);
        assertNotNull(result);
        assertEquals(1, result.a);
    }

    @Test
    public void testPrimitivePropertyWithHandlerPassing() throws IOException {
        final DefaultDeserializationContext dc = new MyDefaultDeserializationContext();

        final ObjectMapper mapper = new ObjectMapper(new JsonFactory(), new DefaultSerializerProvider.Impl(), dc);
        mapper.clearProblemHandlers();
        mapper.addHandler(new IntHandler());
        TestBean result = mapper.readValue(new StringReader("{\"a\": \"non-parsable-int\"}"), TestBean.class);
        assertNotNull(result);
        assertEquals(0, result.a);
    }

    static class IntHandler
            extends DeserializationProblemHandler {
        @Override
        public Object handleWeirdStringValue(DeserializationContext ctxt,
                                             Class<?> targetType,
                                             String valueToConvert,
                                             String failureMsg)
                throws IOException {
            if (targetType != Integer.TYPE) {
                return NOT_HANDLED;
            }
            return 0;
        }
    }

    static class TestBean {

        int a;

        public int getA() {
            return a;
        }

        public void setA(int a) {
            this.a = a;
        }

    }

    private static class MyDefaultDeserializationContext extends DefaultDeserializationContext {
        public MyDefaultDeserializationContext() {
            super(new BeanDeserializerFactory(new DeserializerFactoryConfig()), null);
        }

        @Override
        public Object handleWeirdStringValue(Class<?> targetClass, String value, String msg, Object... msgArgs) throws IOException {
            msg = _format(msg, msgArgs);
            LinkedNode<DeserializationProblemHandler> h = _config.getProblemHandlers();
            while (h != null) {
                Object instance = h.value().handleWeirdStringValue(this, targetClass, value, msg);
                // Remove sane check
                if (instance != DeserializationProblemHandler.NOT_HANDLED) {
                    return instance;
                }
                h = h.next();
            }
            throw weirdStringException(value, targetClass, msg);
        }
        private static final long serialVersionUID = 1L;

        public MyDefaultDeserializationContext(DeserializerFactory df) {
            super(df, null);
        }

        protected MyDefaultDeserializationContext(MyDefaultDeserializationContext src,
                       DeserializationConfig config, JsonParser jp, InjectableValues values) {
            super(src, config, jp, values);
        }

        protected MyDefaultDeserializationContext(MyDefaultDeserializationContext src) { super(src); }

        protected MyDefaultDeserializationContext(MyDefaultDeserializationContext src, DeserializerFactory factory) {
            super(src, factory);
        }

        @Override
        public DefaultDeserializationContext copy() {
            ClassUtil.verifyMustOverride(MyDefaultDeserializationContext.class, this, "copy");
            return new MyDefaultDeserializationContext(this);
        }

        @Override
        public DefaultDeserializationContext createInstance(DeserializationConfig config,
                                                            JsonParser p, InjectableValues values) {
            return new MyDefaultDeserializationContext(this, config, p, values);
        }

        @Override
        public DefaultDeserializationContext with(DeserializerFactory factory) {
            return new MyDefaultDeserializationContext(this, factory);
        }

    }
}

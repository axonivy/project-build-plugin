package test;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Named;

@Named
@ApplicationScoped
public class TestBean {
    public String getString() {
        return "TestBean-String";
    }
}

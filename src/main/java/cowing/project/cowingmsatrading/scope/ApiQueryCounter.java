package cowing.project.cowingmsatrading.scope;

import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
@Getter
class ApiQueryCounter {

    private int count;

    public void increaseCount() {
        count++;
    }
}

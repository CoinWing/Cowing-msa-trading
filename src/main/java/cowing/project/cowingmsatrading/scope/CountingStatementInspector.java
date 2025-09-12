package cowing.project.cowingmsatrading.scope;

import org.hibernate.resource.jdbc.spi.StatementInspector;

public class CountingStatementInspector implements StatementInspector {
    @Override
    public String inspect(String sql) {
        QueryCountContext.increase();
        return sql;
    }
}


package cowing.project.cowingmsatrading.orderbook;

import cowing.project.cowingmsatrading.orderbook.vo.OrderbookUnitVo;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class RealTimeOrderbook {

    private static final String ORDERBOOK_KEY_PREFIX = "orderbook:";
    private static final String ASKS_KEY_SUFFIX = ":asks";
    private static final String BIDS_KEY_SUFFIX = ":bids";

    private final RedisTemplate<String, String> redis;

    public RealTimeOrderbook(@Qualifier("orderbookRedisTemplate") RedisTemplate<String, String> redis) {
        this.redis = redis;
    }

    /**
     * 특정 종목의 매도 호가 정보를 가격 오름차순으로 조회한다.
     * @param code 종목 코드 (e.g., "KRW-BTC")
     * @return 매도 호가 목록
     */
    public List<OrderbookUnitVo> getAskOrderbook(String code) {
        String asksKey = ORDERBOOK_KEY_PREFIX + code + ASKS_KEY_SUFFIX;

        // ZRANGE: Sorted Set에서 index 범위로 오름차순 조회
        Set<ZSetOperations.TypedTuple<String>> askTuples = redis.opsForZSet().rangeWithScores(asksKey, 0, -1);

        validateOrderbook(code, askTuples);

        return askTuples.stream()
                .map(this::convertTupleToVo)
                .collect(Collectors.toList());
    }


    /**
     * 특정 종목의 매수 호가 정보를 가격 내림차순으로 조회한다.
     * @param code 종목 코드 (e.g., "KRW-BTC")
     * @return 매수 호가 목록
     */
    public List<OrderbookUnitVo> getBidOrderbook(String code) {
        String bidsKey = ORDERBOOK_KEY_PREFIX + code + BIDS_KEY_SUFFIX;

        // ZREVRANGE: Sorted Set에서 index 범위로 내림차순 조회
        Set<ZSetOperations.TypedTuple<String>> bidTuples = redis.opsForZSet().reverseRangeWithScores(bidsKey, 0, -1);

        validateOrderbook(code, bidTuples);

        return bidTuples.stream()
                .map(this::convertTupleToVo)
                .collect(Collectors.toList());
    }

    private OrderbookUnitVo convertTupleToVo(ZSetOperations.TypedTuple<String> tuple) {
        // Score(가격)는 Double, Value(수량)는 String으로 저장했으므로 BigDecimal로 변환
        return OrderbookUnitVo.builder()
                .price((Objects.requireNonNull(tuple.getScore())))
                .size((Double.valueOf(Objects.requireNonNull(tuple.getValue()))))
                .build();
    }

    private static void validateOrderbook(String code, Set<ZSetOperations.TypedTuple<String>> askTuples) {
        if (askTuples == null || askTuples.isEmpty()) {
            throw new IllegalArgumentException("실시간 호가가 없습니다. 매매 주문을 처리할 수 없습니다. 종목명:" + code);
        }
    }
}

package com.app.ch1dbindexing;

import com.app.ch1dbindexing.entity.Product;
import com.app.ch1dbindexing.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Slf4j
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class IndexingTest {

    // 데이터 수 설정
    private static final int NUMBER_OF_RECORDS = 100000;

    @PersistenceContext
    private EntityManager entityManager;

    @Autowired
    private ProductRepository productRepository;

    private String searchableProductName;
    private String searchableSerialNumber;

    @BeforeAll
    @Transactional
    @Commit
    void setUp() {
        log.info("PostgreSQL에 테스트 데이터 {}건 생성을 시작합니다...", NUMBER_OF_RECORDS);
        List<Product> products = new ArrayList<>();

        for (int i = 0; i < NUMBER_OF_RECORDS; i++) {
            String uniqueNamePart = UUID.randomUUID().toString().substring(0, 8);
            String uniqueName = "Product " + uniqueNamePart + "_" + i;
            String uniqueSerial = UUID.randomUUID().toString();
            products.add(Product.create(uniqueName, uniqueSerial, "Category " + (i % 100), 10.0 + i));
        }

        productRepository.saveAll(products);
        productRepository.flush();
        log.info("데이터 {}건 생성이 완료되었습니다. 총 레코드 수: {}", NUMBER_OF_RECORDS, productRepository.count());

        // --- 무작위 제품 선택 로직 ---
        long totalProducts = productRepository.count();
        if (totalProducts > 0) {
            Random random = new Random();
            int randomIndex = random.nextInt((int) totalProducts);
            log.debug("총 제품 수: {}. 무작위 인덱스 선택: {}", totalProducts, randomIndex);

            // 선택된 인덱스의 제품을 가져오기 위해 PageRequest 사용
            Page<Product> productPage = productRepository.findAll(PageRequest.of(randomIndex, 1));

            if (productPage.hasContent() && !productPage.getContent().isEmpty()) {
                Product randomProduct = productPage.getContent().get(0);
                searchableProductName = randomProduct.getProductName();
                searchableSerialNumber = randomProduct.getSerialNumber();
                log.info("무작위로 선택된 테스트 대상 제품 - Name: '{}', Serial: '{}'", searchableProductName, searchableSerialNumber);
            } else {
                log.error("무작위 제품 선택 실패 (인덱스: {}). 페이지가 비어있거나 내용이 없습니다.", randomIndex);
                // 테스트가 의미 없으므로 null로 설정하여 assumeTrue에서 걸리도록 함
                searchableProductName = null;
                searchableSerialNumber = null;
            }
        } else {
            log.error("데이터베이스에 제품이 없어 무작위 선택을 할 수 없습니다.");
            searchableProductName = null;
            searchableSerialNumber = null;
        }
        // --- 무작위 제품 선택 로직 종료 ---


        // 여전히 searchableProductName/SerialNumber이 null일 수 있으므로 확인 로직 유지
        if (searchableProductName == null || searchableSerialNumber == null) {
            log.warn("테스트 대상 제품을 설정하지 못했습니다. 테스트가 건너뛰어질 수 있습니다.");
        } else {
            // DB에서 최종 확인
            Optional<Product> checkProduct = productRepository.findBySerialNumber(searchableSerialNumber);
            if (checkProduct.isPresent()) {
                log.info("DB에서 검색 대상 serialNumber '{}' 확인 완료.", searchableSerialNumber);
            } else {
                // 이 경우는 로직상 발생하기 어려우나, 방어적으로 로그 남김
                log.error("DB에서 검색 대상 serialNumber '{}' 를 찾을 수 없습니다. setUp 로직 확인 필요.", searchableSerialNumber);
            }
        }
    }

    @Test
    void testQueryByNonIndexedColumn() {

        if (searchableProductName == null) {
            log.warn("검색할 productName이 설정되지 않았습니다. setUp 확인 필요.");
            assumeTrue(false, "searchableProductName is null");
            return;
        }

        // 영속성 컨텍스트 초기화
        entityManager.clear();
        productRepository.findByProductName("some-non-existing-warm-up-name-to-avoid-cache");

        long startTime = System.nanoTime();
        Optional<Product> product = productRepository.findByProductName(searchableProductName);
        long endTime = System.nanoTime();

        long durationNanos = endTime - startTime;
        log.info("[PostgreSQL - 인덱스 없음] productName '{}' 조회 시간: {} ms ({} ns)",
                searchableProductName,
                TimeUnit.NANOSECONDS.toMillis(durationNanos),
                durationNanos);
        assert product.isPresent() : "Product with name " + searchableProductName + " not found";
    }

    @Test
    void testQueryByIndexedColumn() {
        if (searchableSerialNumber == null) {
            log.warn("검색할 serialNumber가 설정되지 않았습니다. setUp 확인 필요.");
            assumeTrue(false, "searchableSerialNumber is null");
            return;
        }

        // 영속성 컨텍스트 초기화
        entityManager.clear();
        productRepository.findBySerialNumber("some-non-existing-warm-up-serial-to-avoid-cache");

        long startTime = System.nanoTime();
        Optional<Product> product = productRepository.findBySerialNumber(searchableSerialNumber);
        long endTime = System.nanoTime();

        long durationNanos = endTime - startTime;
        log.info("[PostgreSQL - 인덱스 있음] serialNumber '{}' 조회 시간: {} ms ({} ns)",
                searchableSerialNumber,
                TimeUnit.NANOSECONDS.toMillis(durationNanos),
                durationNanos);
        assert product.isPresent() : "Product with serial " + searchableSerialNumber + " not found";
    }

}

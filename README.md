# db-transactional-solution

DB 성능 주제를 장(chapter)별로 실험하는 저장소입니다. 각 장은 독립된 Spring Boot 프로젝트입니다.

| 장 | 주제 | 상태 |
|---|---|---|
| `ch1-db-indexing` | 인덱스 유무에 따른 조회 시간 비교 | 완료 |
| `ch2-db-query-optimization` | 쿼리 최적화 | 프로젝트 뼈대만 있음 |

## ch1-db-indexing

PostgreSQL에 `products` 10만 건을 넣고, 인덱스가 없는 `productName` 과 유니크 인덱스가 있는 `serialNumber` 로 각각 한 건을 조회해 걸린 시간을 비교합니다.

- 테스트 클래스: `src/test/java/com/app/ch1dbindexing/IndexingTest.java`
- 데이터는 `@BeforeAll` 에서 생성하고 커밋합니다. 조회 전에 영속성 컨텍스트를 비우고 워밍업 쿼리를 한 번 날려 캐시 영향을 줄였습니다.
- 결과는 로그로 출력합니다.

```bash
cd ch1-db-indexing
./gradlew test --tests IndexingTest
```

PostgreSQL 접속 정보는 `src/main/resources/application.properties` 에서 맞춥니다.

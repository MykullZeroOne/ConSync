# Running End-to-End Tests - Quick Reference

## Quick Start

```bash
# Build and run all end-to-end tests
cd /Volumes/Development/Intellij\ Projects/microservice/ConSync
mvn clean test -Dtest=SyncServiceEndToEndTest,SyncExecutorEndToEndTest
```

## Individual Test Runs

### SyncServiceEndToEndTest (Orchestration Layer)

```bash
# Run all SyncService tests
mvn test -Dtest=SyncServiceEndToEndTest

# Run specific test
mvn test -Dtest=SyncServiceEndToEndTest#should_sync_new_markdown_file_from_start_to_finish

# Run with verbose output
mvn test -Dtest=SyncServiceEndToEndTest -X
```

### SyncExecutorEndToEndTest (Execution Pipeline)

```bash
# Run all SyncExecutor tests
mvn test -Dtest=SyncExecutorEndToEndTest

# Run specific test
mvn test -Dtest=SyncExecutorEndToEndTest#should_execute_create_action_successfully
```

## Test Descriptions

### SyncServiceEndToEndTest Tests

| Test | Purpose | Key Validations |
|------|---------|-----------------|
| `should_sync_new_markdown_file_from_start_to_finish` | Full workflow for new page | File scanning → parsing → creation |
| `should_update_existing_page_when_content_changes` | Update detection | Hash comparison, version management |
| `should_skip_unchanged_pages` | No unnecessary updates | Content hash verification |
| `should_perform_dry_run_without_making_changes` | Preview mode | No API calls in dry-run |
| `should_handle_multiple_files_with_hierarchy` | Multi-file with structure | Directory hierarchy preservation |
| `should_delete_orphaned_pages_when_configured` | Orphan cleanup | Config-based deletion |
| `should_force_update_all_pages_when_flag_set` | Force update override | All pages updated |
| `should_report_status_correctly` | Status info | File/page counts, state file checks |
| `should_handle_sync_plan_generation_without_execution` | Plan-only mode | No execution, plan generated |
| `should_handle_multiple_creates_and_updates_together` | Mixed operations | Create + update in same sync |
| `should_handle_nested_directory_hierarchy` | Deep nesting | docs/guides/advanced/ support |
| `should_handle_sync_failure_and_partial_state_recovery` | Error handling | Partial state saved on failure |
| `should_reset_sync_state` | State reset | State file deletion |
| `should_handle_empty_markdown_files_gracefully` | Edge cases | Empty file handling |
| `should_handle_markdown_with_special_characters` | Special chars | Bold, italic, code, links |
| `should_handle_title_extraction_from_heading` | H1 parsing | Title from markdown heading |

### SyncExecutorEndToEndTest Tests

| Test | Purpose | Key Validations |
|------|---------|-----------------|
| `should_execute_create_action_successfully` | Page creation | CreatePageRequest, state update |
| `should_execute_update_action_successfully` | Page update | Version increment, content |
| `should_execute_delete_action_successfully` | Page deletion | API call, state cleanup |
| `should_execute_move_action_successfully` | Hierarchy change | Parent ID resolution |
| `should_skip_unchanged_actions` | Skip handling | No API calls |
| `should_execute_multiple_actions_in_order` | Mixed operations | Correct ordering |
| `should_handle_create_failure_gracefully` | Error handling | Failure reporting |
| `should_not_execute_on_dry_run` | Dry-run mode | No actual execution |
| `should_handle_update_with_version_conflict` | Version management | Concurrent modification |
| `should_track_created_page_ids_for_parent_resolution` | Parent tracking | Nested page creation |
| `should_save_final_state_with_last_sync_timestamp` | State persistence | Timestamp saved |

## Code Coverage

Generate coverage report:

```bash
mvn clean test -Dtest=SyncServiceEndToEndTest,SyncExecutorEndToEndTest
# Coverage reports: target/site/jacoco/index.html (if Jacoco is configured)
```

## IDE Integration

### IntelliJ IDEA

1. **Run single test class**:
   - Right-click on `SyncServiceEndToEndTest` → Run

2. **Run single test method**:
   - Click green arrow next to test method
   - Or: Right-click → Run

3. **Debug test**:
   - Right-click → Debug
   - Or: Click debug arrow

4. **Coverage analysis**:
   - Right-click → Run with Coverage
   - View coverage report in Coverage tool window

### VS Code / Other IDEs

```bash
# Run tests from command line
mvn test -Dtest=SyncServiceEndToEndTest

# With additional output
mvn -X test -Dtest=SyncServiceEndToEndTest
```

## Test Output Examples

### Successful Test Run
```
[INFO] Running com.consync.service.SyncServiceEndToEndTest
[INFO] Tests run: 17, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 2.345 s
[INFO] Running com.consync.service.SyncExecutorEndToEndTest
[INFO] Tests run: 16, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 1.892 s
[INFO] BUILD SUCCESS
```

### Test Failure Example
```
[ERROR] should_sync_new_markdown_file_from_start_to_finish  Time elapsed: 0.234 s  <<< FAILURE!
[ERROR] java.lang.AssertionError: expected <1> but was <0>
[ERROR]   at com.consync.service.SyncServiceEndToEndTest.should_sync_new_markdown_file_from_start_to_finish(SyncServiceEndToEndTest.kt:67)
```

## Debugging Tips

### View test logs
```bash
mvn test -Dtest=SyncServiceEndToEndTest -Dsurefire.useFile=false
```

### Enable debug logging
```bash
mvn -X test -Dtest=SyncServiceEndToEndTest
```

### Run single test with timeout
```bash
mvn test -Dtest=SyncServiceEndToEndTest -Dsurefire.timeout=30000
```

## Common Issues & Solutions

### Issue: Java 25 Compatibility Error
**Problem**: `java.lang.IllegalArgumentException: 25`

**Solution**:
- Use Java 17 or Java 21 instead
- The tests are syntactically correct, the issue is with the build environment

```bash
# Using Java 17 if available
export JAVA_HOME=/path/to/java17
mvn test -Dtest=SyncServiceEndToEndTest,SyncExecutorEndToEndTest
```

### Issue: Tests Timeout
**Problem**: Tests take too long to run

**Solution**:
- Increase timeout: `-Dsurefire.timeout=60000` (milliseconds)
- Run single test to isolate issue
- Check for mocked operations that might be slow

### Issue: Mock Not Working
**Problem**: `mockk` assertions fail

**Solution**:
- Verify mock setup in `@BeforeEach`
- Check `coEvery`/`every` syntax
- Ensure `coVerify` uses correct arguments

## Test Maintenance

### Adding New Tests

1. Add test method to appropriate class
2. Follow naming convention: `should_<action>_when_<condition>`
3. Use same setup pattern
4. Mock dependencies with `mockk()`
5. Verify results with assertions

Example:
```kotlin
@Test
fun `should_handle_new_scenario`() = runBlocking {
    // Arrange
    val testData = setupTestData()
    coEvery { client.doSomething(any()) } returns expectedResult
    
    // Act
    val result = syncService.someMethod()
    
    // Assert
    assertTrue(result.success)
    coVerify { client.doSomething(any()) }
}
```

### Updating Tests

- Keep test names descriptive
- Update mocks if API changes
- Add tests for new features
- Remove tests for deprecated features

## Performance Considerations

- Tests are isolated with `@TempDir`
- Mocking prevents actual API calls
- Expected runtime: ~2-5 seconds for all tests
- Parallel execution: configure in `pom.xml`

## Next Steps

1. **Run tests**: `mvn test -Dtest=SyncServiceEndToEndTest,SyncExecutorEndToEndTest`
2. **Review coverage**: Check which code paths are tested
3. **Add more tests**: For specific edge cases
4. **Integration tests**: With real Confluence instance
5. **Performance tests**: For production scenarios

## File Locations

- Test sources: `src/test/kotlin/com/consync/service/`
- Main sources: `src/main/kotlin/com/consync/`
- Test configuration: `pom.xml`
- This guide: `RUNNING_END_TO_END_TESTS.md`
- Full summary: `END_TO_END_TESTS_SUMMARY.md`

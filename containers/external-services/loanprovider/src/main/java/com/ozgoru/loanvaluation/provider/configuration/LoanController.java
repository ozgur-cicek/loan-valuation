package com.ozgoru.loanvaluation.provider.configuration;

import com.ozgoru.loanvaluation.provider.model.LoanDetail;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executor;

@RestController
public class LoanController {

    private static final int[] INSTALLMENTS = new int[]{3, 6, 12};

    private final Executor taskWorkerExecutor;

    private final JdbcTemplate jdbcTemplate;

    public LoanController(DataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS LOAN_DETAILS (id MEDIUMINT AUTO_INCREMENT, " +
                             "cutoff_date date, installment int, amount bigint, PRIMARY KEY (id))");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS LOAN_REQUESTS (id MEDIUMINT AUTO_INCREMENT, " +
                             "export_id varchar(50), cutoff_date date, PRIMARY KEY (id))");

        this.taskWorkerExecutor = taskWorkerExecutor();
    }

    @RequestMapping(
            value = "/loans/export", method = RequestMethod.POST,
            consumes = "application/json"
    )
    @ResponseBody
    public ResponseEntity<String> export(@RequestParam("cutoff_date")
                                         @DateTimeFormat(pattern = "dd.MM.yyyy") Date date) {

        // Check if there is already an export for the given date.
        final String countQuery = "SELECT count(*) FROM LOAN_REQUESTS WHERE cutoff_date = ?;";

        final var count = jdbcTemplate.query(
                countQuery,
                new Object[]{new java.sql.Date(date.getTime())},
                (rs, rowNum) -> rs.getInt(1)
        );

        if (!count.isEmpty() && count.get(0) > 0) {
            System.out.printf("There is an export for the given cutoff_date: %s%n", date);

            return ResponseEntity.status(HttpStatus.CONFLICT).body("{}");
        }

        // Generate new export id.
        final var exportId = UUID.randomUUID();

        // Run batch write on background.
        taskWorkerExecutor.execute(() -> {

            System.out.println("Execution started on background");

            final var numberOfLoans = 30000 + new Random().nextInt(300000);

            List<LoanDetail> loanDetails = new ArrayList<>(numberOfLoans);

            for (int i = 0; i < numberOfLoans; i++) {
                final var loanDetail = new LoanDetail();

                loanDetail.setId(UUID.randomUUID());
                loanDetail.setDate(date);
                loanDetail.setInstallment(INSTALLMENTS[new Random().nextInt(3)]);
                loanDetail.setAmount(10000 + new Random().nextInt(1230000));

                loanDetails.add(loanDetail);
            }

            jdbcTemplate.batchUpdate("INSERT INTO LOAN_DETAILS (cutoff_date, installment, amount) " +
                                     "VALUES (?, ?, ?)",
                    loanDetails,
                    1000,
                    (PreparedStatement ps, LoanDetail product) -> {
                        ps.setDate(1, new java.sql.Date(product.getDate().getTime()));
                        ps.setInt(2, product.getInstallment());
                        ps.setLong(3, product.getAmount());
                    }
            );

            // Write it to the db to mark it is finished.
            jdbcTemplate.update("INSERT INTO LOAN_REQUESTS (export_id, cutoff_date) " +
                                "VALUES (?, ?)",
                    ps -> {
                        ps.setString(1, exportId.toString());
                        ps.setDate(2, new java.sql.Date(date.getTime()));
                    }
            );

            System.out.println("Execution finished on background");
        });

        return ResponseEntity.accepted().body(String.format("{\"export_id\":\"%s\"}", exportId));
    }

    @RequestMapping(
            value = "/loans/status", method = RequestMethod.GET
    )
    @ResponseBody
    public ResponseEntity<String> status(@RequestParam("export_id") String exportId) {

        // Check if export_id is added to the table. If so return Ok.
        final String countQuery = "SELECT count(*) FROM LOAN_REQUESTS WHERE export_id = ?;";

        final var count = jdbcTemplate.query(
                countQuery,
                new Object[]{exportId},
                (rs, rowNum) -> rs.getInt(1)
        );

        if (!count.isEmpty() && count.get(0) > 0) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    private Executor taskWorkerExecutor() {

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(16);
        executor.initialize();
        executor.setThreadNamePrefix("Loan-Exporter-Worker-");

        return executor;
    }
}

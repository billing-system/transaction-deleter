package alphabet.logic;

import dal.TransactionRepository;
import logger.BillingSystemLogger;
import models.db.BillingTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.logging.Level;

@Service
@EnableScheduling
public class TransactionsHandler {

    private final TransactionRepository transactionRepository;
    private final BillingSystemLogger logger;
    private final int numberOfDebits;

    @Autowired
    public TransactionsHandler(TransactionRepository transactionRepository,
                               BillingSystemLogger logger,
                               @Value("${number.of.debits}") int numberOfDebits) {
        this.transactionRepository = transactionRepository;
        this.logger = logger;
        this.numberOfDebits = numberOfDebits;
    }

    @Scheduled(fixedRateString = "${delete.transactions.scheduling.in.ms}")
    public void deleteTransactions() {
        logger.log(Level.FINE, "Going to delete transactions for bank accounts that have successfully paid " +
                "off all their debits");

        try {
            tryDeletingTransactions();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Unknown exception has occurred while trying to delete transactions: " +
                    e.getMessage());
        }
    }

    private void tryDeletingTransactions() {
        List<String> dstBankAccountsThatPayedAllDebits =
                transactionRepository.findBankAccountsThatFinishedPayingDebits(numberOfDebits);
        deleteTransactionsForBankAccounts(dstBankAccountsThatPayedAllDebits);

        if (!dstBankAccountsThatPayedAllDebits.isEmpty()) {
            logger.log(Level.FINER, "Deleted all transactions for the following bank accounts: " +
                    dstBankAccountsThatPayedAllDebits);
            logger.log(Level.INFO, "Deleted all transactions for " +
                    dstBankAccountsThatPayedAllDebits.size() + " bank accounts");
        }
    }

    private void deleteTransactionsForBankAccounts(List<String> bankAccounts) {
        List<BillingTransaction> transactionsToDelete = transactionRepository.findByDstBankAccountIn(bankAccounts);
        transactionRepository.deleteAll(transactionsToDelete);
    }
}

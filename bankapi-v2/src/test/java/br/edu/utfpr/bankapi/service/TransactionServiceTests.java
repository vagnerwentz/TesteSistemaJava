package br.edu.utfpr.bankapi.service;

import br.edu.utfpr.bankapi.dto.DepositDTO;
import br.edu.utfpr.bankapi.dto.TransferDTO;
import br.edu.utfpr.bankapi.dto.WithdrawDTO;
import br.edu.utfpr.bankapi.exception.NotFoundException;
import br.edu.utfpr.bankapi.model.Account;
import br.edu.utfpr.bankapi.model.Transaction;
import br.edu.utfpr.bankapi.model.TransactionType;
import br.edu.utfpr.bankapi.repository.AccountRepository;
import br.edu.utfpr.bankapi.repository.TransactionRepository;
import br.edu.utfpr.bankapi.validations.AvailableAccountValidation;
import br.edu.utfpr.bankapi.validations.AvailableBalanceValidation;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;

import java.util.Optional;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class TransactionServiceTests {
    @InjectMocks
    TransactionService transactionService;
    @Mock
    TransactionRepository transactionRepository;
    @Mock
    AccountRepository accountRepository;

    @Mock
    private AvailableAccountValidation availableAccountValidation;

    @Mock
    private AvailableBalanceValidation availableBalanceValidation;



    @Captor
    ArgumentCaptor<Transaction> transactionCaptor;
    @DisplayName("A conta de destino deve existir para uma transação.")
    @Test
    public void shouldExistTargetAccount() throws Exception {
        var dto = new DepositDTO(
                12345, 1000
        );
        var receiverAccount = new Account("Elo Pinga", 12345, 0.0, 0);

        BDDMockito.given(accountRepository.getByNumber(12345)).willReturn(Optional.of(receiverAccount));

        transactionService.deposit(dto);

        BDDMockito.then(transactionRepository).should().save(transactionCaptor.capture());
        Transaction transaction = transactionCaptor.getValue();

        Assertions.assertEquals(receiverAccount, transaction.getReceiverAccount());
        Assertions.assertEquals(1000, transaction.getAmount());
        Assertions.assertEquals(TransactionType.DEPOSIT, transaction.getType());
        Assertions.assertNotEquals(receiverAccount.getBalance(), 0);
    }

    @Test
    public void shouldWithdraw() throws NotFoundException {
        // Mock data
        long sourceAccountNumber = 123456;
        double initialBalance = 500.0;
        double withdrawalAmount = 100.0;
        double expectedFinalBalance = initialBalance - withdrawalAmount;

        Account sourceAccount = new Account();
        sourceAccount.setNumber(sourceAccountNumber);
        sourceAccount.setBalance(initialBalance);

        WithdrawDTO withdrawDTO = new WithdrawDTO(sourceAccountNumber, withdrawalAmount);

        Transaction transaction = new Transaction();
        BeanUtils.copyProperties(withdrawDTO, transaction);
        transaction.setType(TransactionType.WITHDRAW);
        transaction.setSourceAccount(sourceAccount);

        when(availableAccountValidation.validate(sourceAccountNumber)).thenReturn(sourceAccount);

        // Perform withdraw
        transactionService.withdraw(withdrawDTO);
        Assertions.assertEquals(expectedFinalBalance, sourceAccount.getBalance());
    }

    @Test
    public void testTransfer() throws NotFoundException {
        // Mock data
        long sourceAccountNumber = 123456;
        long receiverAccountNumber = 654321;
        double initialSourceBalance = 500.0;
        double initialReceiverBalance = 200.0;
        double transferAmount = 100.0;
        double expectedFinalSourceBalance = initialSourceBalance - transferAmount;
        double expectedFinalReceiverBalance = initialReceiverBalance + transferAmount;

        Account sourceAccount = new Account();
        sourceAccount.setNumber(sourceAccountNumber);
        sourceAccount.setBalance(initialSourceBalance);

        Account receiverAccount = new Account();
        receiverAccount.setNumber(receiverAccountNumber);
        receiverAccount.setBalance(initialReceiverBalance);

        TransferDTO transferDTO = new TransferDTO(sourceAccountNumber, receiverAccountNumber, transferAmount);

        Transaction transaction = new Transaction();
        BeanUtils.copyProperties(transferDTO, transaction);
        transaction.setType(TransactionType.TRANSFER);
        transaction.setSourceAccount(sourceAccount);
        transaction.setReceiverAccount(receiverAccount);

        when(availableAccountValidation.validate(sourceAccountNumber)).thenReturn(sourceAccount);
        when(availableAccountValidation.validate(receiverAccountNumber)).thenReturn(receiverAccount);

        // Perform transfer
        Transaction savedTransaction = transactionService.transfer(transferDTO);

        // Verify source account balance deduction
        Assertions.assertEquals(expectedFinalSourceBalance, sourceAccount.getBalance());

        // Verify receiver account balance increment
        Assertions.assertEquals(expectedFinalReceiverBalance, receiverAccount.getBalance());
    }
}

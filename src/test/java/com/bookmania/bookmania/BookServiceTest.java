package com.bookmania.bookmania;

import com.bookmania.bookmania.Dtos.BookRequest;
import com.bookmania.bookmania.Dtos.BookResponse;
import com.bookmania.bookmania.Entity.Book;
import com.bookmania.bookmania.Entity.Category;
import com.bookmania.bookmania.Exception.BusinessException;
import com.bookmania.bookmania.Exception.ResourceNotFoundException;
import com.bookmania.bookmania.Repository.BookRepository;
import com.bookmania.bookmania.Repository.CategoryRepository;
import com.bookmania.bookmania.Services.BookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;
    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private BookService bookService;

    private Book book;
    private Category category;
    private BookRequest request;

    @BeforeEach
    void setUp() {
        category = Category.builder().id(2L).name("Ficción").build();

        book = Book.builder()
                .id(1L)
                .title("Clean Code")
                .author("Robert C. Martin")
                .isbn("978-0132350884")
                .totalCopies(3)
                .availableCopies(3)
                .categories(Set.of(category))
                .build();

        request = new BookRequest();
        request.setTitle("Clean Code");
        request.setAuthor("Robert C. Martin");
        request.setIsbn("978-0132350884");
        request.setTotalCopies(3);
        request.setCategoryIds(Set.of(2L));
    }

    @Test
    void getById_existing_returnsBookWithCategoryNames() {
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        BookResponse result = bookService.getById(1L);

        assertThat(result.getTitle()).isEqualTo("Clean Code");
        assertThat(result.getCategories()).containsExactly("Ficción");
    }

    @Test
    void getById_missing_throwsResourceNotFoundException() {
        when(bookRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.getById(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void create_newIsbn_savesBookWithAvailableCopiesEqualToTotal() {
        when(bookRepository.existsByIsbn("978-0132350884")).thenReturn(false);
        when(categoryRepository.findAllById(Set.of(2L))).thenReturn(List.of(category));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        BookResponse result = bookService.create(request);

        assertThat(result.getAvailableCopies()).isEqualTo(3);
        assertThat(result.getTotalCopies()).isEqualTo(3);
    }

    @Test
    void create_duplicateIsbn_throwsBusinessException() {
        when(bookRepository.existsByIsbn("978-0132350884")).thenReturn(true);

        assertThatThrownBy(() -> bookService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ISBN");
    }

    @Test
    void create_noValidCategories_throwsResourceNotFoundException() {
        when(bookRepository.existsByIsbn("978-0132350884")).thenReturn(false);
        when(categoryRepository.findAllById(Set.of(2L))).thenReturn(List.of());

        assertThatThrownBy(() -> bookService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_totalCopiesUnchanged_leavesAvailableCopiesAlone() {
        book.setAvailableCopies(1); // one currently on loan
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(categoryRepository.findAllById(Set.of(2L))).thenReturn(List.of(category));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        request.setTitle("Clean Code (2nd ed.)"); // totalCopies stays 3, same as book's
        BookResponse result = bookService.update(1L, request);

        assertThat(result.getTitle()).isEqualTo("Clean Code (2nd ed.)");
        assertThat(result.getAvailableCopies()).isEqualTo(1);
    }

    @Test
    void update_totalCopiesIncreased_shiftsAvailableCopiesByTheSameDelta() {
        // book: 3 total, 1 available (2 on loan) -> restocked to 5 total
        book.setAvailableCopies(1);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(categoryRepository.findAllById(Set.of(2L))).thenReturn(List.of(category));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        request.setTotalCopies(5);
        BookResponse result = bookService.update(1L, request);

        // +2 total copies -> the 2 new copies are immediately available, not stuck at 1
        assertThat(result.getAvailableCopies()).isEqualTo(3);
        assertThat(result.getTotalCopies()).isEqualTo(5);
    }

    @Test
    void update_totalCopiesDecreasedBelowOnLoanCount_flooredAtZeroInsteadOfNegative() {
        // book: 3 total, 1 available (2 on loan) -> shrunk to 1 total
        book.setAvailableCopies(1);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(categoryRepository.findAllById(Set.of(2L))).thenReturn(List.of(category));
        when(bookRepository.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        request.setTotalCopies(1);
        BookResponse result = bookService.update(1L, request);

        assertThat(result.getAvailableCopies()).isZero();
    }

    @Test
    void update_missing_throwsResourceNotFoundException() {
        when(bookRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> bookService.update(1L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void delete_existing_deletesBook() {
        when(bookRepository.existsById(1L)).thenReturn(true);

        bookService.delete(1L);

        verify(bookRepository).deleteById(1L);
        verify(bookRepository).flush();
    }

    @Test
    void delete_missing_throwsResourceNotFoundException() {
        when(bookRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> bookService.delete(1L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(bookRepository, never()).deleteById(any());
    }

    @Test
    void delete_bookWithLoanOrReservationHistory_throwsBusinessExceptionInsteadOfRaw500() {
        // A book with any Loan/Reservation row still pointing at it (even
        // returned/cancelled ones - there's no cascade) fails its FK
        // constraint on delete; that used to surface as an uncaught
        // DataIntegrityViolationException -> generic 500.
        when(bookRepository.existsById(1L)).thenReturn(true);
        doThrow(new DataIntegrityViolationException("FK violation")).when(bookRepository).flush();

        assertThatThrownBy(() -> bookService.delete(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("préstamos, reservas o multas");
    }
}

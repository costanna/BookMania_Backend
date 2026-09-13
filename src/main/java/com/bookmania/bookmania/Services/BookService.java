package com.bookmania.bookmania.Services;

import com.bookmania.bookmania.Dtos.BookRequest;
import com.bookmania.bookmania.Dtos.BookResponse;
import com.bookmania.bookmania.Entity.Book;
import com.bookmania.bookmania.Entity.Category;
import com.bookmania.bookmania.Exception.BusinessException;
import com.bookmania.bookmania.Exception.ResourceNotFoundException;
import com.bookmania.bookmania.Repository.BookRepository;
import com.bookmania.bookmania.Repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;

    public List<BookResponse> getAll() {
        return bookRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public Page<BookResponse> getFiltered(String title, String author, Long categoryId,
            int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("title").ascending());
        return bookRepository.findWithFilters(title, categoryId, pageable)
                .map(this::toResponse);
    }

    public BookResponse getById(Long id) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado"));
        return toResponse(book);
    }

    public BookResponse create(BookRequest request) {
        if (bookRepository.existsByIsbn(request.getIsbn())) {
            throw new BusinessException("Ya existe un libro con ese ISBN");
        }

        Set<Category> categories = new HashSet<>(
                categoryRepository.findAllById(request.getCategoryIds())
        );
        if (categories.isEmpty()) {
            throw new ResourceNotFoundException("No se encontraron categorías válidas");
        }

        Book book = new Book();
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setIsbn(request.getIsbn());
        book.setPages(request.getPages());
        book.setPublishYear(request.getPublishYear());
        book.setCoverUrl(request.getCoverUrl());
        book.setTotalCopies(request.getTotalCopies());
        book.setAvailableCopies(request.getTotalCopies());
        book.setCategories(categories);

        return toResponse(bookRepository.save(book));
    }

    public BookResponse update(Long id, BookRequest request) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado"));

        Set<Category> categories = new HashSet<>(
                categoryRepository.findAllById(request.getCategoryIds())
        );
        if (categories.isEmpty()) {
            throw new ResourceNotFoundException("No se encontraron categorías válidas");
        }

        // totalCopies can change here, but availableCopies is never touched
        // by anything else in this method - without shifting it by the same
        // delta, raising totalCopies (restocking) silently left the new
        // copies unavailable to loan, and lowering it could leave
        // availableCopies sitting above the new totalCopies entirely.
        int delta = request.getTotalCopies() - book.getTotalCopies();
        book.setAvailableCopies(Math.max(0, book.getAvailableCopies() + delta));

        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setIsbn(request.getIsbn());
        book.setPages(request.getPages());
        book.setPublishYear(request.getPublishYear());
        book.setCoverUrl(request.getCoverUrl());
        book.setTotalCopies(request.getTotalCopies());
        book.setCategories(categories);

        return toResponse(bookRepository.save(book));
    }

    public void delete(Long id) {
        if (!bookRepository.existsById(id)) {
            throw new ResourceNotFoundException("Libro no encontrado");
        }
        try {
            // flush() forces the DELETE (and any FK check) to run right here
            // instead of being deferred to end-of-transaction, where it would
            // surface as an uncaught DataIntegrityViolationException nowhere
            // near this try/catch and fall through to a generic 500.
            bookRepository.deleteById(id);
            bookRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(
                    "No se puede eliminar el libro: tiene préstamos, reservas o multas asociadas"
            );
        }
    }

    private BookResponse toResponse(Book book) {
        Set<String> categoryNames = book.getCategories().stream()
                .map(Category::getName)
                .collect(Collectors.toSet());

        return new BookResponse(
                book.getId(),
                book.getTitle(),
                book.getAuthor(),
                book.getIsbn(),
                book.getPages(),
                book.getPublishYear(),
                book.getCoverUrl(),
                book.getTotalCopies(),
                book.getAvailableCopies(),
                categoryNames
        );
    }
}

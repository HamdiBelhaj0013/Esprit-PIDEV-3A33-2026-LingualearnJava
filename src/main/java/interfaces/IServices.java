package interfaces;

import java.util.List;

public interface IServices<T> {

    void add(T t) throws Exception;

    void update(T t) throws Exception;

    void delete(int id) throws Exception;

    List<T> getAll() throws Exception;

    T getById(int id) throws Exception;

    List<T> search(String keyword) throws Exception;
}
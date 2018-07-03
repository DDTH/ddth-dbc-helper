package com.github.ddth.dao;

import java.util.stream.Stream;

import com.github.ddth.dao.utils.DaoException;
import com.github.ddth.dao.utils.DaoResult;

/**
 * API interface for DAO that manages one single BO class.
 * 
 * @author Thanh Nguyen <btnguyen2k@gmail.com>
 * @since 0.8.0
 */
public interface IGenericBoDao<T> {
    /**
     * Create/Persist a new BO to storage.
     * 
     * @param bo
     * @return
     * @throws DaoException
     */
    DaoResult create(T bo) throws DaoException;

    /**
     * Delete an existing BO from storage.
     * 
     * @param bo
     * @return
     * @throws DaoException
     */
    DaoResult delete(T bo) throws DaoException;

    /**
     * Fetch an existing BO from storage by id.
     * 
     * @param id
     * @return
     * @throws DaoException
     */
    T get(BoId id) throws DaoException;

    /**
     * Fetch list of existing BOs from storage by id.
     * 
     * @param idList
     * @return
     * @throws DaoException
     */
    T[] get(BoId... idList) throws DaoException;

    /**
     * Fetch all existing BOs from storage and return the result as a stream.
     * 
     * @return
     * @since 0.9.0
     * @throws DaoException
     */
    Stream<T> getAll() throws DaoException;

    /**
     * Fetch all existing BOs from storage, sorted by primary key(s) and return the result as a
     * stream.
     * 
     * @return
     * @since 0.9.0
     * @throws DaoException
     */
    Stream<T> getAllSorted() throws DaoException;

    /**
     * Update an existing BO.
     * 
     * @param bo
     * @return
     * @throws DaoException
     */
    DaoResult update(T bo) throws DaoException;

    /**
     * Create a new BO or update an existing one.
     * 
     * @param bo
     * @return
     * @since 0.8.1
     * @throws DaoException
     */
    DaoResult createOrUpdate(T bo) throws DaoException;

    /**
     * Update an existing BO or create a new one.
     * 
     * @param bo
     * @return
     * @since 0.8.1
     * @throws DaoException
     */
    DaoResult updateOrCreate(T bo) throws DaoException;
}

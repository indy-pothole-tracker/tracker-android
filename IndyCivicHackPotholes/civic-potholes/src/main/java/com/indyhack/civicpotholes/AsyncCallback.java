package com.indyhack.civicpotholes;

/**
 * Created by david on 6/16/14 for IndyCivicHackPotholes
 */
public interface AsyncCallback<T> {

    public void onResult(T result);

    public void onFail();
}

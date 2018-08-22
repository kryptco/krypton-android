package co.krypt.krypton.u2f;

import co.krypt.krypton.exception.InvalidAppIdException;

public interface FacetProvider {
    public String[] getFacetsFromAppId(String appId) throws InvalidAppIdException;
}

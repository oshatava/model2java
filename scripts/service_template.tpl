package {PACKAGE_NAME};

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import rx.Observable;

{IMPORTS}

/**
 * Created by {USER_NAME} on {DATE}.
 */
{ANNOTATIONS}
public interface {ENTITY_NAME}Service {IMPLEMENTS} {

//Please paste your code here, to be sure code not disappear when script will be completed
/*---------------------------------------CUSTOMER CODE START--------------------------------------*/
{CUSTOMER_CODE}
/*---------------------------------------CUSTOMER CODE END----------------------------------------*/

/*-----------------------------------------------------------------------------------------------*/

    @GET("{PLURAL}/{id}")
    {CALL_TYPE}<{ENTITY_NAME}> get(@Path("id")int id);

    @GET("{PLURAL}/{id}?filter={\"include\":[{INCLUDE_ALL_RELATIONS}]}")
    {CALL_TYPE}<{ENTITY_NAME}> getFull(@Path("id")int id);

    @GET("{PLURAL}")
    {CALL_TYPE}<List<{ENTITY_NAME}>> list();

    @POST("{PLURAL}")
    {CALL_TYPE}<{ENTITY_NAME}> create(@Body {ENTITY_NAME} value);

    @PUT("{PLURAL}/{id}")
    {CALL_TYPE}<{ENTITY_NAME}> update(@Path("id")int id, @Body {ENTITY_NAME} value);

    @DELETE("{PLURAL}/{id}")
    {CALL_TYPE}<{ENTITY_NAME}> delete(@Path("id")int id);

/*-----------------------------------------------------------------------------------------------*/
    {BODY}

}


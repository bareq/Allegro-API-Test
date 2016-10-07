/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package allegroapitest;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.rpc.ServiceException;
import newAllegroApi.ArrayOfCategorytreetype;
import newAllegroApi.ArrayOfFilteroptionstype;
import newAllegroApi.ArrayOfString;
import newAllegroApi.CategoriesListType;
import newAllegroApi.CategoryData;
import newAllegroApi.CategoryTreeType;
import newAllegroApi.DoGetCategoryPathRequest;
import newAllegroApi.DoGetCategoryPathResponse;
import newAllegroApi.DoGetItemsListRequest;
import newAllegroApi.DoGetItemsListResponse;
import newAllegroApi.DoLoginRequest;
import newAllegroApi.DoLoginResponse;
import newAllegroApi.DoQuerySysStatusRequest;
import newAllegroApi.DoQuerySysStatusResponse;
import newAllegroApi.FilterOptionsType;
import newAllegroApi.ServicePort;
import newAllegroApi.ServiceService;
import newAllegroApi.ServiceService_Impl;
import newAllegroApi.SortOptionsType;

/**
 *
 * @author bartoszlach
 */
public class AllegroApiTest {

    public static final long CARS_ID = 4029; //id kategorii przeszukiwanej
    public static final String webapiKey = "<WEB_API_KEY>"; // klucz webapi
    public static final int countryCode = 1; //kod kraju - dostajemy go razem z kluczem webapi 
    public static final String login = "<LOGIN>"; //login allegro
    public static final String password = "<PASSWORD>"; //haslo allegro
    public static long localVersion = -1;
    public static List<KeyValue> lista;

    public static void main(String[] args) {
        ServiceService allegroService = new ServiceService_Impl();
        lista = new ArrayList();
        for (int i = 0; i < 10; i++) {
            lista.add(new KeyValue("tmp", 0));
        }

        try {
            ServicePort allegro = allegroService.getServicePort();

            DoQuerySysStatusRequest doQuerySysStatusRequest = new DoQuerySysStatusRequest(3, countryCode, webapiKey);
            DoQuerySysStatusResponse doQuerySysStatus = allegro.doQuerySysStatus(doQuerySysStatusRequest);
            localVersion = doQuerySysStatus.getVerKey();

            String sessionId = logowanie(allegro);
            przeszukuj(allegro, sessionId, CARS_ID);

        } catch (ServiceException ex) {
            Logger.getLogger(AllegroApiTest.class.getName()).log(Level.SEVERE, null, ex);
        } catch (RemoteException ex) {
            Logger.getLogger(AllegroApiTest.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public static void przeszukuj(ServicePort allegro, String session, long categoryId) {
        FilterOptionsType filterOptionsType = new FilterOptionsType("category", new ArrayOfString(new String[]{String.valueOf(categoryId)}), null);
        ArrayOfFilteroptionstype filterOptions = new ArrayOfFilteroptionstype(new FilterOptionsType[]{filterOptionsType});
        SortOptionsType sortOptions = new SortOptionsType();

        DoGetItemsListRequest doGetItemsListRequest = new DoGetItemsListRequest(webapiKey, countryCode, filterOptions, sortOptions, 0, 0, 5);
        DoGetItemsListResponse doGetItemsListResponse;
        while (true) {
            try {
                doGetItemsListResponse = allegro.doGetItemsList(doGetItemsListRequest);
                break;
            } catch (RemoteException ex) {
                System.err.println("DoGetItemsListRequest");
                logowanie(allegro);
            }
        }
        CategoriesListType categoriesListType = doGetItemsListResponse.getCategoriesList();
        ArrayOfCategorytreetype categoriesTree = categoriesListType.getCategoriesTree();
        for (CategoryTreeType ctt : categoriesTree.getItem()) {
            if (ctt.getCategoryParentId() == categoryId && ctt.getCategoryItemsCount() > 0) {
                DoGetCategoryPathRequest categoryPathRequest = new DoGetCategoryPathRequest(session, ctt.getCategoryId());
                DoGetCategoryPathResponse categoryPathResponse;
                while (true) {
                    try {
                        categoryPathResponse = allegro.doGetCategoryPath(categoryPathRequest);
                        break;
                    } catch (RemoteException ex) {
                        System.err.println("doGetCategoryPath");
                        logowanie(allegro);
                    }
                }
                String nazwa = "";
                boolean complete = false;
                for (CategoryData aoc : categoryPathResponse.getCategoryPath().getItem()) {
                    if (aoc.getCatLevel() > 2) {
                        nazwa += aoc.getCatName() + "/";
                    }
                    if (aoc.getCatIsLeaf() == 1) {
                        complete = true;
                        //System.out.println(nazwa + " " + ctt.getCategoryItemsCount());
                        boolean zmiana = false;
                        for (int i = 9; i >= 0; i--) {
                            if (lista.get(i).getValue() < ctt.getCategoryItemsCount()) {
                                zmiana = true;
                                KeyValue tmp = lista.get(i);
                                lista.set(i, new KeyValue(nazwa, ctt.getCategoryItemsCount()));
                                if (i < 9) {
                                    lista.set(i + 1, tmp);
                                }
                            } else {
                                break;
                            }
                        }
                        if (zmiana) {
                            System.out.println();

                            for (int i = 0; i < lista.size(); i++) {
                                System.out.println(i + 1 + " " + lista.get(i).getName() + " " + lista.get(i).getValue());
                            }
                        }
                    }
                }
                if (!complete) {
                    przeszukuj(allegro, session, ctt.getCategoryId());
                }
            }
        }
    }

    public static String logowanie(ServicePort allegro) {
        DoLoginRequest doLoginRequest = new DoLoginRequest(login, password, countryCode, webapiKey, localVersion);
        DoLoginResponse doLoginResponse = null;
        while (true) {
            try {
                doLoginResponse = allegro.doLogin(doLoginRequest);
                break;
            } catch (RemoteException ex) {
                System.err.println("Logowanie");
            }
        }
        return doLoginResponse.getSessionHandlePart();
    }

}

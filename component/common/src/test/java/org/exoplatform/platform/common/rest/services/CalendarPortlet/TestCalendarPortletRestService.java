package org.exoplatform.platform.common.rest.services.CalendarPortlet;

import org.exoplatform.calendar.ws.bean.ErrorResource;
import org.exoplatform.platform.common.rest.services.BaseRestServicesTestCase;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.impl.EnvironmentContext;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.test.mock.MockHttpServletRequest;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.json.JSONArray;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

public class TestCalendarPortletRestService extends BaseRestServicesTestCase {

    protected Class<?> getComponentClass() {
        return CalendarPortletRestService.class;
    }

    public void testGetDispNonDispCals() throws Exception {
        String path = "/portlet/HomePageCalendarPortlet/settings";
        EnvironmentContext envctx = new EnvironmentContext();
        HttpServletRequest httpRequest =
                new MockHttpServletRequest(path, null, 0, "GET", null);

        envctx.put(HttpServletRequest.class, httpRequest);
        Identity identity = new Identity("root");
        ConversationState.setCurrent(new ConversationState(identity));
        Map<String, Object> spacesMap = new HashMap<String, Object>();
        SpaceService ss = createProxy(SpaceService.class, spacesMap);
        getContainer().registerComponentInstance("SpaceService", ss);
        ContainerResponse resp =
                launcher.service("GET", path, "", null, null, envctx);
        assertEquals(200, resp.getStatus());
        String response = resp.getEntity().toString();
        JSONObject responseObject = new JSONObject(response);
        assertNotNull(responseObject.get("allDisplayedCals"));
        JSONArray displayedArray = (JSONArray) responseObject.get("allDisplayedCals");
        assertEquals(displayedArray.length(), 1);
        assertEquals(new JSONObject(displayedArray.get(0).toString()).get("id"), "idGroups");
        assertNotNull(responseObject.get("nonDisplayedCals"));
        JSONArray nonDisplayedArray = (JSONArray) responseObject.get("nonDisplayedCals");
        assertEquals(nonDisplayedArray.length(), 1);
        assertEquals(new JSONObject(nonDisplayedArray.get(0).toString()).get("id"), "idUser");
    }

    public void testGetDispNonDispCalsFromSpace() throws Exception {
        String path = "/portlet/HomePageCalendarPortlet/settings?spaceId=space1";
        EnvironmentContext envctx = new EnvironmentContext();
        HttpServletRequest httpRequest =
                new MockHttpServletRequest(path, null, 0, "GET", null);

        envctx.put(HttpServletRequest.class, httpRequest);
        Identity identity = new Identity("root");
        ConversationState.setCurrent(new ConversationState(identity));
        Map<String, Object> spacesMap = new HashMap<String, Object>();
        Space space1 = new Space();
        space1.setPrettyName("space1");
        space1.setId("space1");
        space1.setGroupId("/spaces/space1");
        space1.setVisibility(Space.HIDDEN);
        space1.setMembers(new String[] { "root" });
        spacesMap.put("getSpaceById", null);
        SpaceService ss = createProxy(SpaceService.class, spacesMap);
        getContainer().unregisterComponent("SpaceService");
        getContainer().registerComponentInstance("SpaceService", ss);
        ContainerResponse resp =
                launcher.service("GET", path, "", null, null, envctx);
        //Test non existing space
        assertEquals(404, resp.getStatus());
        assertEquals(((ErrorResource) resp.getEntity()).getMessage(), "space space1 not found");
        assertEquals(((ErrorResource) resp.getEntity()).getDeveloperMessage(), "space not found");
        spacesMap.put("getSpaceById",  space1);
        ss = createProxy(SpaceService.class, spacesMap);
        getContainer().unregisterComponent("SpaceService");
        getContainer().registerComponentInstance("SpaceService", ss);
        resp = launcher.service("GET", path, "", null, null, envctx);
        assertEquals(200, resp.getStatus());
        String response = resp.getEntity().toString();
        JSONObject responseObject = new JSONObject(response);
        assertNotNull(responseObject.get("allDisplayedCals"));
        JSONArray displayedArray = (JSONArray) responseObject.get("allDisplayedCals");
        assertEquals(displayedArray.length(), 1);
        assertEquals(new JSONObject(displayedArray.get(0).toString()).get("id"), "idGroups");
    }
}

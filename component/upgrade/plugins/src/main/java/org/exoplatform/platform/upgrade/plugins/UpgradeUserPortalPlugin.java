package org.exoplatform.platform.upgrade.plugins;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.navigation.*;
import org.exoplatform.portal.mop.page.PageService;
import org.exoplatform.portal.pom.data.ModelDataStorage;
import org.exoplatform.portal.pom.data.PortalData;
import org.exoplatform.portal.pom.data.PortalKey;
import org.exoplatform.services.jcr.RepositoryService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;

public class UpgradeUserPortalPlugin extends AbstractGadgetToPortletPlugin {

    private static final Log LOG = ExoLogger.getLogger(UpgradeUserPortalPlugin.class);

    private OrganizationService orgService;

    public UpgradeUserPortalPlugin(SettingService settingService,
                                   ModelDataStorage modelDataStorage,
                                   PageService pageService,
                                   NavigationService navigationService,
                                   OrganizationService orgService,
                                   RepositoryService repoService,
                                   InitParams initParams) {
        super(settingService, modelDataStorage, pageService, navigationService, repoService, initParams);
        this.orgService = orgService;
    }

    public UpgradeUserPortalPlugin(InitParams initParams) {
        super(initParams);
    }

    @Override
    public void processUpgrade(String oldVersion, String newVersion) {
        LOG.info("Migrate Gadgets on user sites layout");
        this.migratePortals(SiteType.USER);

        this.cleanupUserNavs();
    }

    private void cleanupUserNavs() {
        int size = 0;
        try {
            ListAccess<User> listUser = this.orgService.getUserHandler().findAllUsers();
            size = listUser.getSize();
            LOG.info("START remove navigations/pages for " + size + " user sites");

            int limit = 100;
            int index = 0;

            do {
                final PortalContainer container = PortalContainer.getInstance();
                RequestLifeCycle.begin(container);

                int max = (index + limit >= size) ? size - index : limit;

                User[] users = listUser.load(index, max);

                List<Future> futures = new ArrayList<>();
                for (final User user : users) {

                    Runnable task = () -> {
                        final String username = user.getUserName().trim();
                        try {
                            ExoContainerContext.setCurrentContainer(container);
                            RequestLifeCycle.begin(container);
                            if (username.isEmpty()) {
                                return;
                            }
                            LOG.info("START clean up navigations and pages of user site: " + username);
                            PortalData portal = modelDataStorage.getPortalConfig(new PortalKey(PortalConfig.USER_TYPE, user.getUserName()));
                            if (portal == null) {
                                LOG.info("Site for user " + username + " does not exists");
                            } else {
                                NavigationContext navContext = this.navigationService.loadNavigation(SiteKey.user(user.getUserName()));
                                this.removeNavs(navContext);
                            }
                            LOG.info("DONE clean up navigations and pages for user site: " + user.getUserName());

                        } catch (Exception ex) {
                            LOG.error("Error while clean up page/navigation of user: " + username, ex);
                        } finally {
                            RequestLifeCycle.end();
                        }
                    };

                    futures.add(executor.submit(task));
                }
                for (Future f : futures) {
                    f.get();
                }

                RequestLifeCycle.end();

                index += max;

            } while (index < size);

        } catch (Exception ex) {
            LOG.error("Error while migrate user pages", ex);
        } finally {
            LOG.info("DONE remove navigations/pages for " + size + " user sites");
        }
    }

    private void removeNavs(NavigationContext context) {
        NodeContext node = this.navigationService.loadNode(NodeModel.SELF_MODEL, context, Scope.ALL, null);

        this.removeNavNode(node);

        this.navigationService.saveNavigation(context);
    }

    private void removeNavNode(NodeContext node) {
        LOG.info("Try to remove node: " + node.getName());
        if (node.getNodeSize() > 0) {
            LOG.info("Remove children of " + node.getName());
            Iterator<NodeContext> iterator = node.iterator();
            while (iterator.hasNext()) {
                this.removeNavNode(iterator.next());
            }
            LOG.info("Done remove children of " + node.getName());
            navigationService.saveNode(node, null);
        }

        if (node.getParent() != null) {
            org.exoplatform.portal.mop.page.PageKey pageKey = node.getState().getPageRef();
            if (pageKey != null) {
                LOG.info("Try to remove page " + pageKey.format() + " reference from navigation " + node.getName());
                pageService.destroyPage(pageKey);
            }

            LOG.info("Remove navigation node " + node.getName());
            node.removeNode();
        }
    }
}

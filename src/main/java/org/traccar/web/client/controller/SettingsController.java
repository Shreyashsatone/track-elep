/*
 * Copyright 2013 Anton Tananaev (anton.tananaev@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.traccar.web.client.controller;

import java.util.ArrayList;
import java.util.List;

import com.sencha.gxt.data.shared.Store;
import org.traccar.web.client.Application;
import org.traccar.web.client.ApplicationContext;
import org.traccar.web.client.i18n.Messages;
import org.traccar.web.client.model.BaseAsyncCallback;
import org.traccar.web.client.model.UserProperties;
import org.traccar.web.client.view.*;
import org.traccar.web.shared.model.ApplicationSettings;
import org.traccar.web.shared.model.User;

import com.google.gwt.core.client.GWT;
import com.sencha.gxt.data.shared.ListStore;
import com.sencha.gxt.widget.core.client.Dialog.PredefinedButton;
import com.sencha.gxt.widget.core.client.box.AlertMessageBox;
import com.sencha.gxt.widget.core.client.box.ConfirmMessageBox;
import com.sencha.gxt.widget.core.client.event.DialogHideEvent;

public class SettingsController implements DeviceView.SettingsHandler {

    private Messages i18n = GWT.create(Messages.class);
    private final UserSettingsDialog.UserSettingsHandler userSettingsHandler;

    public SettingsController(UserSettingsDialog.UserSettingsHandler userSettingsHandler) {
        this.userSettingsHandler = userSettingsHandler;
    }

    @Override
    public void onAccountSelected() {
        new UserDialog(
                ApplicationContext.getInstance().getUser(),
                new UserDialog.UserHandler() {
                    @Override
                    public void onSave(User user) {
                        Application.getDataService().updateUser(user, new BaseAsyncCallback<User>(i18n) {
                            @Override
                            public void onSuccess(User result) {
                                ApplicationContext.getInstance().setUser(result);
                            }
                        });
                    }
                }).show();
    }

    @Override
    public void onPreferencesSelected() {
        new UserSettingsDialog(ApplicationContext.getInstance().getUserSettings(), userSettingsHandler).show();
    }

    @Override
    public void onUsersSelected() {
        Application.getDataService().getUsers(new BaseAsyncCallback<List<User>>(i18n) {
            @Override
            public void onSuccess(List<User> result) {
                UserProperties userProperties = GWT.create(UserProperties.class);
                final ListStore<User> userStore = new ListStore<User>(userProperties.id());
                userStore.addAll(result);

                new UsersDialog(userStore, new UsersDialog.UserHandler() {

                    @Override
                    public void onAdd() {
                        new UserDialog(
                                new User(),
                                new UserDialog.UserHandler() {
                                    @Override
                                    public void onSave(User user) {
                                        Application.getDataService().addUser(user, new BaseAsyncCallback<User>(i18n) {
                                            @Override
                                            public void onSuccess(User result) {
                                                userStore.add(result);
                                            }
                                            @Override
                                            public void onFailure(Throwable caught) {
                                                new AlertMessageBox(i18n.error(), i18n.errUsernameTaken()).show();
                                            }
                                        });
                                    }
                                }).show();
                    }

                    @Override
                    public void onRemove(final User user) {
                        final ConfirmMessageBox dialog = new ConfirmMessageBox(i18n.confirm(), i18n.confirmUserRemoval());
                        dialog.addDialogHideHandler(new DialogHideEvent.DialogHideHandler() {
							@Override
							public void onDialogHide(DialogHideEvent event) {
								if (event.getHideButton() == PredefinedButton.YES) {
                                    Application.getDataService().removeUser(user, new BaseAsyncCallback<User>(i18n) {
                                        @Override
                                        public void onSuccess(User result) {
                                            userStore.remove(user);
                                        }
                                    });
								}
							}
						});
                        dialog.show();
                    }

                    @Override
                    public void onSaveRoles() {
                        List<User> updatedUsers = new ArrayList<User>(userStore.getModifiedRecords().size());
                        for (Store<User>.Record record : userStore.getModifiedRecords()) {
                            User updatedUser = new User(record.getModel());
                            for (Store.Change<User, ?> change : record.getChanges()) {
                                change.modify(updatedUser);
                            }
                            updatedUsers.add(updatedUser);
                        }

                        Application.getDataService().saveRoles(updatedUsers, new BaseAsyncCallback<Void>(i18n) {
                            @Override
                            public void onSuccess(Void result) {
                                userStore.commitChanges();
                            }
                        });
                    }
                }).show();
            }
        });
    }

    @Override
    public void onApplicationSelected() {
        new ApplicationSettingsDialog(
                ApplicationContext.getInstance().getApplicationSettings(),
                new ApplicationSettingsDialog.ApplicationSettingsHandler() {
                    @Override
                    public void onSave(final ApplicationSettings applicationSettings) {
                        Application.getDataService().updateApplicationSettings(applicationSettings, new BaseAsyncCallback<Void>(i18n) {
                            @Override
                            public void onSuccess(Void result) {
                                ApplicationContext.getInstance().setApplicationSettings(applicationSettings);
                            }
                        });
                    }
                }).show();
    }

}

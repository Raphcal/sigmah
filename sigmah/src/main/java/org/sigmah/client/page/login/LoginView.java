/*
 * All Sigmah code is released under the GNU General Public License v3
 * See COPYRIGHT.txt and LICENSE.txt.
 */

package org.sigmah.client.page.login;

import com.extjs.gxt.ui.client.event.BaseEvent;
import com.extjs.gxt.ui.client.event.Events;
import com.extjs.gxt.ui.client.event.Listener;
import com.extjs.gxt.ui.client.widget.MessageBox;
import com.extjs.gxt.ui.client.widget.button.Button;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.PasswordTextBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlowPanel;
import org.sigmah.client.i18n.I18N;

/**
 *
 * @author Raphaël Calabro (rcalabro@ideia.fr)
 */
public class LoginView extends Composite {

    public LoginView() {
        final SimplePanel panel = new SimplePanel();
        panel.setStyleName("login-background");

        final Grid grid = new Grid(1, 2);
        grid.setStyleName("login-box");

        // Logo
        grid.setWidget(0, 0, new Image("image/login-logo.png"));

        // Form
        final FlexTable form = new FlexTable();
        form.setWidth("90%");

        // E-Mail field
        form.setText(0, 0, I18N.CONSTANTS.loginLoginField());
        form.getCellFormatter().setStyleName(0, 0, "login-box-form-label");

        final TextBox loginTextBox = new TextBox();
        loginTextBox.setWidth("100%");
        form.setWidget(0, 1, loginTextBox);
        form.getFlexCellFormatter().setColSpan(0, 1, 2);

        // Separator
        for(int i = 0; i < 3; i++)
            form.getCellFormatter().setStyleName(1, i, "login-box-form-separator");

        // Password field
        form.setText(2, 0, I18N.CONSTANTS.loginPasswordField());
        form.getCellFormatter().setStyleName(2, 0, "login-box-form-label");

        final PasswordTextBox passwordTextBox = new PasswordTextBox();
        passwordTextBox.setWidth("100%");
        form.setWidget(2, 1, passwordTextBox);
        form.getFlexCellFormatter().setColSpan(2, 1, 2);

        // Separator
        for(int i = 0; i < 3; i++)
            form.getCellFormatter().setStyleName(3, i, "login-box-form-separator");

        // Password forgotten link
        final FlowPanel bottomPanel = new FlowPanel();
        bottomPanel.getElement().getStyle().setPosition(Position.RELATIVE);

        final Anchor label = new Anchor(I18N.CONSTANTS.loginPasswordForgotten());
        label.setStyleName("login-box-form-forgotten");
        label.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                MessageBox.alert("Error", "Not Implemented Yet.", null);
            }
        });
        bottomPanel.add(label);

        final Image loader = new Image("image/login-loader.gif");
        loader.getElement().getStyle().setVisibility(Visibility.HIDDEN);
        loader.getElement().getStyle().setMarginTop(-8, Unit.PX);
        loader.getElement().getStyle().setPosition(Position.ABSOLUTE);
        loader.getElement().getStyle().setTop(50, Unit.PCT);
        loader.getElement().getStyle().setRight(2, Unit.PX);
        bottomPanel.add(loader);

        form.setWidget(4, 0, bottomPanel);
        form.getFlexCellFormatter().setColSpan(4, 0, 2);

        // Login button
        final Button loginButton = new Button(I18N.CONSTANTS.loginConnectButton());
        loginButton.setWidth("120px");
        form.setWidget(4, 1, loginButton);
        form.getCellFormatter().setHorizontalAlignment(4, 1, HasHorizontalAlignment.ALIGN_RIGHT);

        // Login actions
        loginButton.addListener(Events.Select, new Listener<BaseEvent>() {
            @Override
            public void handleEvent(BaseEvent be) {
                doLogin(loginTextBox.getText(), passwordTextBox.getText(), loginButton, loader);
            }
        });

        final KeyDownHandler handler = new KeyDownHandler() {
            @Override
            public void onKeyDown(KeyDownEvent event) {
                if(event.getNativeKeyCode() == KeyCodes.KEY_ENTER)
                    doLogin(loginTextBox.getText(), passwordTextBox.getText(), loginButton, loader);
            }
        };
        loginTextBox.addKeyDownHandler(handler);
        passwordTextBox.addKeyDownHandler(handler);
        
        // Adding the form to the orange box
        grid.getCellFormatter().setHorizontalAlignment(0, 1, HasHorizontalAlignment.ALIGN_CENTER);
        grid.setWidget(0, 1, form);

        // Styles
        grid.getCellFormatter().setStyleName(0, 0, "login-box-logo");
        grid.getCellFormatter().setStyleName(0, 1, "login-box-form");

        panel.add(grid);

        initWidget(panel);
    }

    private void doLogin(final String login, final String password, final Button loginButton, final Image loader) {
        final String query = "email="+login+"&password="+password;
        final RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.POST,"../Login/service?"+query);
        requestBuilder.setCallback(new RequestCallback() {
            @Override
            public void onResponseReceived(Request request, Response response) {
                if(response.getText().contains("OK"))
                    Window.Location.reload();
                else {
                    MessageBox.alert(I18N.CONSTANTS.loginConnectErrorTitle(), I18N.CONSTANTS.loginConnectErrorBadLogin(), null);
                    loginButton.setEnabled(true);
                    loader.getElement().getStyle().setVisibility(Visibility.HIDDEN);
                }
            }

            @Override
            public void onError(Request request, Throwable exception) {
                MessageBox.alert(I18N.CONSTANTS.loginConnectErrorTitle(), exception.getMessage(), null);
                loginButton.setEnabled(true);
                loader.getElement().getStyle().setVisibility(Visibility.HIDDEN);
            }
        });

        loginButton.setEnabled(false);
        loader.getElement().getStyle().setVisibility(Visibility.VISIBLE);
        try {
            requestBuilder.send();
        } catch (RequestException ex) {
           MessageBox.alert(I18N.CONSTANTS.loginConnectErrorTitle(), ex.getMessage(), null);
        }
    }
}
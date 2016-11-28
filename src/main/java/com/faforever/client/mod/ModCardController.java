package com.faforever.client.mod;

import com.faforever.client.fx.Controller;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.reporting.ReportingService;
import javafx.collections.ListChangeListener;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.function.Consumer;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class ModCardController implements Controller<Node> {

  public Label commentsLabel;
  public ImageView thumbnailImageView;
  public Label nameLabel;
  public Label authorLabel;
  public Label likesLabel;
  public Node modTileRoot;

  @Inject
  ModService modService;
  @Inject
  NotificationService notificationService;
  @Inject
  I18n i18n;
  @Inject
  ReportingService reportingService;

  private ModInfoBean mod;
  private Consumer<ModInfoBean> onOpenDetailListener;
  private ListChangeListener<ModInfoBean> installStatusChangeListener;

  public void initialize() {
    installStatusChangeListener = change -> {
      while (change.next()) {
        for (ModInfoBean modInfoBean : change.getAddedSubList()) {
          if (mod.getId().equals(modInfoBean.getId())) {
            setInstalled(true);
            return;
          }
        }
        for (ModInfoBean modInfoBean : change.getRemoved()) {
          if (mod.getId().equals(modInfoBean.getId())) {
            setInstalled(false);
            return;
          }
        }
      }
    };
  }

  private void setInstalled(boolean installed) {

  }

  public void setMod(ModInfoBean mod) {
    this.mod = mod;
    thumbnailImageView.setImage(modService.loadThumbnail(mod));
    nameLabel.setText(mod.getName());
    authorLabel.setText(mod.getAuthor());
    likesLabel.setText(String.format("%d", mod.getLikes()));
    commentsLabel.setText(String.format("%d", mod.getComments().size()));
    modService.getInstalledMods().addListener(installStatusChangeListener);
  }

  public Node getRoot() {
    return modTileRoot;
  }

  public void setOnOpenDetailListener(Consumer<ModInfoBean> onOpenDetailListener) {
    this.onOpenDetailListener = onOpenDetailListener;
  }

  public void onShowModDetail() {
    onOpenDetailListener.accept(mod);
  }
}

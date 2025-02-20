/* Copyright (c) 2016-2022 Jesper Öqvist <jesper@llbit.se>
 * Copyright (c) 2016-2022 Chunky Contributors
 *
 * This file is part of Chunky.
 *
 * Chunky is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Chunky is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with Chunky.  If not, see <http://www.gnu.org/licenses/>.
 */
package se.llbit.chunky.ui.render.tabs;

import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar.ButtonData;
import javafx.scene.paint.Color;
import se.llbit.chunky.renderer.EmitterSamplingStrategy;
import se.llbit.chunky.renderer.SunSamplingStrategy;
import se.llbit.chunky.renderer.scene.Scene;
import se.llbit.chunky.renderer.scene.sky.Sky;
import se.llbit.chunky.renderer.scene.sky.Sun;
import se.llbit.chunky.ui.elements.AngleAdjuster;
import se.llbit.chunky.ui.DoubleAdjuster;
import se.llbit.chunky.ui.controller.RenderControlsFxController;
import se.llbit.chunky.ui.render.RenderControlsTab;
import se.llbit.fx.LuxColorPicker;
import se.llbit.fxutil.Dialogs;
import se.llbit.math.ColorUtil;
import se.llbit.math.QuickMath;
import se.llbit.util.Registerable;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class LightingTab extends ScrollPane implements RenderControlsTab, Initializable {
  private RenderControlsFxController controller;
  private Scene scene;

  @FXML private DoubleAdjuster skyExposure;
  @FXML private DoubleAdjuster skyIntensity;
  @FXML private DoubleAdjuster apparentSkyBrightness;
  @FXML private DoubleAdjuster emitterIntensity;
  @FXML private DoubleAdjuster sunIntensity;
  @FXML private CheckBox drawSun;
  @FXML private ComboBox<SunSamplingStrategy> sunSamplingStrategy;
  @FXML private TitledPane importanceSamplingDetailsPane;
  @FXML private DoubleAdjuster importanceSampleChance;
  @FXML private DoubleAdjuster importanceSampleRadius;
  @FXML private DoubleAdjuster sunLuminosity;
  @FXML private DoubleAdjuster apparentSunBrightness;
  @FXML private DoubleAdjuster sunRadius;
  @FXML private AngleAdjuster sunAzimuth;
  @FXML private AngleAdjuster sunAltitude;
  @FXML private CheckBox enableEmitters;
  @FXML private LuxColorPicker sunColor;
  @FXML private LuxColorPicker apparentSunColor;
  @FXML private CheckBox modifySunTexture;
  @FXML private ChoiceBox<EmitterSamplingStrategy> emitterSamplingStrategy;

  private ChangeListener<Color> sunColorListener = (observable, oldValue, newValue) -> scene.sun().setColor(ColorUtil.fromFx(newValue));

  private ChangeListener<Color> apparentSunColorListener = (observable, oldValue, newValue) -> scene.sun().setApparentColor(ColorUtil.fromFx(newValue));

  public LightingTab() throws IOException {
    FXMLLoader loader = new FXMLLoader(getClass().getResource("LightingTab.fxml"));
    loader.setRoot(this);
    loader.setController(this);
    loader.load();
  }

  @Override public void initialize(URL location, ResourceBundle resources) {
    skyExposure.setName("天空曝光");
    skyExposure.setTooltip("改变天空的曝光度。");
    skyExposure.setRange(Sky.MIN_INTENSITY, Sky.MAX_INTENSITY);
    skyExposure.makeLogarithmic();
    skyExposure.clampMin();
    skyExposure.onValueChange(value -> scene.sky().setSkyExposure(value));

    skyIntensity.setName("天空光照强度调节");
    skyIntensity.setTooltip("修改天空发出的光的强度。");
    skyIntensity.setRange(Sky.MIN_INTENSITY, Sky.MAX_INTENSITY);
    skyIntensity.makeLogarithmic();
    skyIntensity.clampMin();
    skyIntensity.onValueChange(value -> scene.sky().setSkyLight(value));

    apparentSkyBrightness.setName("天空表观亮度调节");
    apparentSkyBrightness.setTooltip("修改天空的表观亮度。");
    apparentSkyBrightness.setRange(Sky.MIN_APPARENT_INTENSITY, Sky.MAX_APPARENT_INTENSITY);
    apparentSkyBrightness.makeLogarithmic();
    apparentSkyBrightness.clampMin();
    apparentSkyBrightness.onValueChange(value -> scene.sky().setApparentSkyLight(value));

    enableEmitters.setTooltip(new Tooltip("允许基于材质设置的方块发光。"));
    enableEmitters.selectedProperty().addListener(
      (observable, oldValue, newValue) -> scene.setEmittersEnabled(newValue));

    emitterIntensity.setName("发射器强度");
    emitterIntensity.setTooltip("修改发射器光的强度。");
    emitterIntensity.setRange(Scene.MIN_EMITTER_INTENSITY, Scene.MAX_EMITTER_INTENSITY);
    emitterIntensity.makeLogarithmic();
    emitterIntensity.clampMin();
    emitterIntensity.onValueChange(value -> scene.setEmitterIntensity(value));

    emitterSamplingStrategy.getItems().addAll(EmitterSamplingStrategy.values());
    emitterSamplingStrategy.getSelectionModel().selectedItemProperty()
      .addListener((observable, oldvalue, newvalue) -> {
        scene.setEmitterSamplingStrategy(newvalue);
        if (newvalue != EmitterSamplingStrategy.NONE && scene.getEmitterGrid() == null && scene.haveLoadedChunks()) {
          Alert warning = Dialogs.createAlert(AlertType.CONFIRMATION);
          warning.setContentText("The selected chunks need to be reloaded in order for emitter sampling to work.");
          warning.getButtonTypes().setAll(
            ButtonType.CANCEL,
            new ButtonType("重载区块", ButtonData.FINISH));
          warning.setTitle("需要重载区块");
          ButtonType result = warning.showAndWait().orElse(ButtonType.CANCEL);
          if (result.getButtonData() == ButtonData.FINISH) {
            controller.getRenderController().getSceneManager().reloadChunks();
          }
        }
      });
    emitterSamplingStrategy.setTooltip(new Tooltip("决定每次反射时如何对发射器进行采样。"));

    drawSun.selectedProperty().addListener((observable, oldValue, newValue) -> scene.sun().setDrawTexture(newValue));
    drawSun.setTooltip(new Tooltip("在skymap上绘制太阳材质。"));

    for (SunSamplingStrategy strategy : SunSamplingStrategy.values()) {
      if (strategy.getDeprecationStatus() != Registerable.DeprecationStatus.HIDDEN) {
        sunSamplingStrategy.getItems().add(strategy);
      }
    }
    sunSamplingStrategy.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> {
              scene.setSunSamplingStrategy(newValue);

              boolean visible = scene != null && scene.getSunSamplingStrategy().isImportanceSampling();
              importanceSamplingDetailsPane.setVisible(visible);
              importanceSamplingDetailsPane.setExpanded(visible);
              importanceSamplingDetailsPane.setManaged(visible);
            });
    sunSamplingStrategy.setTooltip(new Tooltip("Determines how the sun is sampled at each bounce."));

    boolean visible = scene != null && scene.getSunSamplingStrategy().isImportanceSampling();
    importanceSamplingDetailsPane.setVisible(visible);
    importanceSamplingDetailsPane.setExpanded(visible);
    importanceSamplingDetailsPane.setManaged(visible);

    importanceSampleChance.setName("重要性采样机会");
    importanceSampleChance.setTooltip("在每次重要性反射中采样太阳的概率。");
    importanceSampleChance.setRange(Sun.MIN_IMPORTANCE_SAMPLE_CHANCE, Sun.MAX_IMPORTANCE_SAMPLE_CHANCE);
    importanceSampleChance.clampBoth();
    importanceSampleChance.onValueChange(value -> scene.sun().setImportanceSampleChance(value));

    importanceSampleRadius.setName("重要性采样半径");
    importanceSampleRadius.setTooltip("太阳可能采样反射半径（相对于太阳的半径）。");
    importanceSampleRadius.setRange(Sun.MIN_IMPORTANCE_SAMPLE_RADIUS, Sun.MAX_IMPORTANCE_SAMPLE_RADIUS);
    importanceSampleRadius.clampMin();
    importanceSampleRadius.onValueChange(value -> scene.sun().setImportanceSampleRadius(value));

    sunIntensity.setName("阳光强度");
    sunIntensity.setTooltip("改变阳光的强度。仅在太阳采样策略设置为FAST或HIGH_QUALITY时使用。");
    sunIntensity.setRange(Sun.MIN_INTENSITY, Sun.MAX_INTENSITY);
    sunIntensity.makeLogarithmic();
    sunIntensity.clampMin();
    sunIntensity.onValueChange(value -> scene.sun().setIntensity(value));

    sunLuminosity.setName("太阳亮度");
    sunLuminosity.setTooltip("改变太阳的绝对亮度。仅在太阳采样策略设置为OFF或HIGH_QUALITY时使用。");    sunLuminosity.setRange(1, 10000);
    sunLuminosity.makeLogarithmic();
    sunLuminosity.clampMin();
    sunLuminosity.onValueChange(value -> scene.sun().setLuminosity(value));

    apparentSunBrightness.setName("太阳表观亮度");
    apparentSunBrightness.setTooltip("改变太阳材质的表观亮度。");
    apparentSunBrightness.setRange(Sun.MIN_APPARENT_BRIGHTNESS, Sun.MAX_APPARENT_BRIGHTNESS);
    apparentSunBrightness.makeLogarithmic();
    apparentSunBrightness.clampMin();
    apparentSunBrightness.onValueChange(value -> scene.sun().setApparentBrightness(value));

    sunRadius.setName("太阳大小");
    sunRadius.setTooltip("太阳半径（以度为单位）。");
    sunRadius.setRange(0.01, 20);
    sunRadius.clampMin();
    sunRadius.onValueChange(value -> scene.sun().setSunRadius(Math.toRadians(value)));

    sunColor.colorProperty().addListener(sunColorListener);

    modifySunTexture.setTooltip(new Tooltip("控制太阳材质的颜色是否受太阳表观颜色的影响。"));
    modifySunTexture.selectedProperty().addListener((observable, oldValue, newValue) -> {
      scene.sun().setEnableTextureModification(newValue);
      apparentSunColor.setDisable(!newValue);
    });

    apparentSunColor.setDisable(true);
    apparentSunColor.colorProperty().addListener(apparentSunColorListener);

    sunAzimuth.setName("太阳方位角");
    sunAzimuth.setTooltip("改变太阳相对于以东部作为参考方向时的水平方向。");
    sunAzimuth.onValueChange(value -> scene.sun().setAzimuth(-QuickMath.degToRad(value)));

    sunAltitude.setName("太阳高度");
    sunAltitude.setTooltip("改变太阳相对于以地平线作为参考高度时的垂直方向。");
    sunAltitude.onValueChange(value -> scene.sun().setAltitude(QuickMath.degToRad(value)));
  }

  @Override
  public void setController(RenderControlsFxController controller) {
    this.controller = controller;
    scene = controller.getRenderController().getSceneManager().getScene();
  }

  @Override public void update(Scene scene) {
    skyExposure.set(scene.sky().getSkyExposure());
    skyIntensity.set(scene.sky().getSkyLight());
    apparentSkyBrightness.set(scene.sky().getApparentSkyLight());
    emitterIntensity.set(scene.getEmitterIntensity());
    sunIntensity.set(scene.sun().getIntensity());
    sunLuminosity.set(scene.sun().getLuminosity());
    apparentSunBrightness.set(scene.sun().getApparentBrightness());
    sunRadius.set(Math.toDegrees(scene.sun().getSunRadius()));
    modifySunTexture.setSelected(scene.sun().getEnableTextureModification());
    sunAzimuth.set(-QuickMath.radToDeg(scene.sun().getAzimuth()));
    sunAltitude.set(QuickMath.radToDeg(scene.sun().getAltitude()));
    enableEmitters.setSelected(scene.getEmittersEnabled());
    sunSamplingStrategy.getSelectionModel().select(scene.getSunSamplingStrategy());
    importanceSampleChance.set(scene.sun().getImportanceSampleChance());
    importanceSampleRadius.set(scene.sun().getImportanceSampleRadius());
    drawSun.setSelected(scene.sun().drawTexture());
    sunColor.colorProperty().removeListener(sunColorListener);
    sunColor.setColor(ColorUtil.toFx(scene.sun().getColor()));
    sunColor.colorProperty().addListener(sunColorListener);
    apparentSunColor.colorProperty().removeListener(apparentSunColorListener);
    apparentSunColor.setColor(ColorUtil.toFx(scene.sun().getApparentColor()));
    apparentSunColor.colorProperty().addListener(apparentSunColorListener);
    emitterSamplingStrategy.getSelectionModel().select(scene.getEmitterSamplingStrategy());
  }

  @Override public String getTabTitle() {
    return "Lighting";
  }

  @Override public Node getTabContent() {
    return this;
  }
}

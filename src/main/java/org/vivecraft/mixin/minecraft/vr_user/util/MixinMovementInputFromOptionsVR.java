package org.vivecraft.mixin.minecraft.vr_user.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.main.Main;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.util.MovementInput;
import net.minecraft.util.MovementInputFromOptions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.vivecraft.control.VRInputAction;
import org.vivecraft.gameplay.screenhandlers.KeyboardHandler;
import org.vivecraft.intermediaries.interfaces.client.IMinecraftVR;
import org.vivecraft.intermediaries.interfaces.util.IMovementInputFromOptionsVR;
import org.vivecraft.provider.MCOpenVR;
import org.vivecraft.settings.VRSettings;
import org.vivecraft.utils.Utils;
import org.vivecraft.utils.Vector2;
import org.vivecraft.utils.jkatvr;

@Mixin(MovementInputFromOptions.class)
public class MixinMovementInputFromOptionsVR extends MovementInput implements IMovementInputFromOptionsVR {

    @Shadow
    private GameSettings gameSettings;
    private boolean autoSprintActive = false;
    private boolean movementSetByAnalog = false;

    private static float getMovementAxisValue(KeyBinding keyBinding) {
        VRInputAction action = MCOpenVR.getInputAction(keyBinding);
        return Math.abs(action.getAxis1DUseTracked());
    }

    private float axisToDigitalMovement(float value) {
        if (value > 0.5f)
            return 1;
        if (value < -0.5f)
            return -1;
        return 0;
    }

    @Overwrite
    public void updatePlayerMoveState() {
        this.moveStrafe = 0.0F;
        this.moveForward = 0.0F;
        Minecraft mc = Minecraft.getMinecraft();

        boolean flag = false;
        if (((IMinecraftVR)mc).getClimbTracker().isClimbeyClimb() && !mc.player.isInWater() && ((IMinecraftVR)mc).getClimbTracker().isGrabbingLadder())
            flag = true; // movement not allowed while climbing

        if (!flag && (this.gameSettings.keyBindForward.isKeyDown() || MCOpenVR.keyTeleportFallback.isKeyDown())) {
            ++this.moveForward;
            this.forwardKeyDown = true;
        } else {
            this.forwardKeyDown = false;
        }

        if (!flag && this.gameSettings.keyBindBack.isKeyDown()) {
            --this.moveForward;
            this.backKeyDown = true;
        } else {
            this.backKeyDown = false;
        }

        if (!flag && this.gameSettings.keyBindLeft.isKeyDown()) {
            ++this.moveStrafe;
            this.leftKeyDown = true;
        } else {
            this.leftKeyDown = false;
        }

        if (!flag && this.gameSettings.keyBindRight.isKeyDown()) {
            --this.moveStrafe;
            this.rightKeyDown = true;
        } else {
            this.rightKeyDown = false;
        }

        boolean setMovement = false;
        float forwardAxis = 0;
        if (!flag && !((IMinecraftVR)mc).getVRSettings().seated && mc.currentScreen == null && !KeyboardHandler.Showing){
            // override everything

            VRInputAction strafeAction = MCOpenVR.getInputAction(MCOpenVR.keyFreeMoveStrafe);
            VRInputAction rotateAction = MCOpenVR.getInputAction(MCOpenVR.keyFreeMoveRotate);
            Vector2 strafeAxis = strafeAction.getAxis2DUseTracked();
            Vector2 rotateAxis = rotateAction.getAxis2DUseTracked();

            if (strafeAxis.getX() != 0 || strafeAxis.getY() != 0) {
                setMovement = true;
                forwardAxis = strafeAxis.getY();
                if (((IMinecraftVR)mc).getVRSettings().analogMovement) {
                    this.moveForward = strafeAxis.getY();
                    this.moveStrafe = -strafeAxis.getX();
                } else {
                    this.moveForward = axisToDigitalMovement(strafeAxis.getY());
                    this.moveStrafe = axisToDigitalMovement(-strafeAxis.getX());
                }
            } else if (rotateAxis.getY() != 0) {
                setMovement = true;
                forwardAxis = rotateAxis.getY();
                if (((IMinecraftVR)mc).getVRSettings().analogMovement) {
                    this.moveForward = rotateAxis.getY();

                    this.moveStrafe = 0;
                    this.moveStrafe -= getMovementAxisValue(this.gameSettings.keyBindRight);
                    this.moveStrafe += getMovementAxisValue(this.gameSettings.keyBindLeft);
                } else {
                    this.moveForward = axisToDigitalMovement(rotateAxis.getY());
                }
            } else if (((IMinecraftVR)mc).getVRSettings().analogMovement) {
                setMovement = true;
                this.moveForward = 0;
                this.moveStrafe = 0;

                float forward = getMovementAxisValue(this.gameSettings.keyBindForward);
                if (forward == 0) forward = getMovementAxisValue(MCOpenVR.keyTeleportFallback);
                forwardAxis = forward;

                this.moveForward += forward;
                this.moveForward -= getMovementAxisValue(this.gameSettings.keyBindBack);
                this.moveStrafe -= getMovementAxisValue(this.gameSettings.keyBindRight);
                this.moveStrafe += getMovementAxisValue(this.gameSettings.keyBindLeft);

                float deadzone = 0.05f;
                this.moveForward = Utils.applyDeadzone(this.moveForward, deadzone);
                this.moveStrafe = Utils.applyDeadzone(this.moveStrafe, deadzone);
            }

            if(setMovement) {
                // just assuming all this below is needed for compatibility.
                this.forwardKeyDown = this.moveForward > 0;
                this.backKeyDown = this.moveForward < 0;
                this.leftKeyDown = this.moveStrafe > 0;
                this.rightKeyDown = this.moveStrafe < 0;
                VRInputAction.setKeyBindState(this.gameSettings.keyBindForward, this.forwardKeyDown);
                VRInputAction.setKeyBindState(this.gameSettings.keyBindBack, this.backKeyDown);
                VRInputAction.setKeyBindState(this.gameSettings.keyBindLeft, this.leftKeyDown);
                VRInputAction.setKeyBindState(this.gameSettings.keyBindRight, this.rightKeyDown);

                if (((IMinecraftVR)mc).getVRSettings().autoSprint) {
                    // Sprint only works for walk forwards obviously
                    if (forwardAxis >= ((IMinecraftVR)mc).getVRSettings().autoSprintThreshold) {
                        mc.player.setSprinting(true);
                        autoSprintActive = true;
                        this.moveForward = 1;
                    } else if (this.moveForward > 0 && ((IMinecraftVR)mc).getVRSettings().analogMovement) {
                        // Adjust range so you can still reach full speed while not sprinting
                        this.moveForward = (this.moveForward / ((IMinecraftVR)mc).getVRSettings().autoSprintThreshold) * 1.0f;
                    }
                }
            }
        }

        if (!setMovement) {
            if (movementSetByAnalog) {
                VRInputAction.setKeyBindState(this.gameSettings.keyBindForward, false);
                VRInputAction.setKeyBindState(this.gameSettings.keyBindBack, false);
                VRInputAction.setKeyBindState(this.gameSettings.keyBindLeft, false);
                VRInputAction.setKeyBindState(this.gameSettings.keyBindRight, false);
            }
        }
        movementSetByAnalog = setMovement;

        if (autoSprintActive && forwardAxis < ((IMinecraftVR)mc).getVRSettings().autoSprintThreshold) {
            mc.player.setSprinting(false);
            autoSprintActive = false;
        }

        boolean ok = mc.currentScreen == null && (((IMinecraftVR)mc).getVRPlayer().getFreeMove() || ((IMinecraftVR)mc).getVRSettings().simulateFalling) && !flag;

        // VIVECRAFT DO ok.
        this.jump = this.gameSettings.keyBindJump.isKeyDown() && ok;

        this.sneak = (((IMinecraftVR)mc).getSneakTracker().sneakCounter > 0 || ((IMinecraftVR)mc).getSneakTracker().sneakOverride || this.gameSettings.keyBindSneak.isKeyDown())
                && mc.currentScreen == null;


        if (this.sneak)
        {
            this.moveStrafe = (float) ((double) this.moveStrafe * 0.3D);
            this.moveForward = (float) ((double) this.moveForward * 0.3D);
        }

        // VIVECRAFT ADDITIONS ***
        VRSettings vr =((IMinecraftVR)mc).getVRSettings();
        this.moveForward = this.moveForward * vr.movementSpeedMultiplier;
        this.moveStrafe = this.moveStrafe * vr.movementSpeedMultiplier;

//        if (Main.katvr && !flag) {
//            this.moveStrafe = 0;
//            this.moveForward = jkatvr.getSpeed() * jkatvr.walkDirection() * vr.movementSpeedMultiplier;
//        }

    }

}

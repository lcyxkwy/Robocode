package sample;

import robocode.*;
import java.awt.*;
import java.util.*;
import robocode.util.Utils;

public class YanAn extends AdvancedRobot {
    private double enemyBearing;
    private double enemyDistance;
    private String lockedEnemy;
    private boolean isEvading;
    private Random rand = new Random();
    private int direction = 1;
    private long lastScanTime = 0;

    public void run() {
        setAdjustRadarForGunTurn(true);
        setAdjustGunForRobotTurn(true);
        setColors(Color.ORANGE, Color.BLUE, Color.RED, Color.WHITE, Color.GRAY);

        while (true) {
            if (lockedEnemy == null) {
                setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
            }

            if (!isEvading) {
                smartCircleMovement();
            }

            execute();
        }
    }

    private void smartCircleMovement() {
        double wallMargin = 100;
        double fieldWidth = getBattleFieldWidth();
        double fieldHeight = getBattleFieldHeight();
        double x = getX();
        double y = getY();

        if (x < wallMargin || x > fieldWidth - wallMargin || 
            y < wallMargin || y > fieldHeight - wallMargin) {
            
            double escapeAngle = Math.atan2(
                (fieldWidth/2 - x), 
                (fieldHeight/2 - y)
            );
            
            setTurnRightRadians(Utils.normalRelativeAngle(
                escapeAngle - getHeadingRadians()
            ));
            setAhead(150);
            execute();
            return;
        }

        setTurnRight(90 - (getHeading() % 360) + direction * 10);
        setAhead(100);
        direction *= -1;
        
        if (rand.nextInt(10) == 0) {
            setMaxTurnRate(rand.nextInt(10) + 5);
        }
    }

    public void onScannedRobot(ScannedRobotEvent e) {
        if (e.getEnergy() <= 0) return;

        if (lockedEnemy == null || 
            (!e.getName().equals(lockedEnemy) && e.getDistance() < enemyDistance)) {
            
            lockedEnemy = e.getName();
            enemyDistance = e.getDistance();
            enemyBearing = e.getBearingRadians();
        }

        if (!e.getName().equals(lockedEnemy)) return;

        double radarTurn = Utils.normalRelativeAngle(
            (getHeadingRadians() + e.getBearingRadians()) - 
            getRadarHeadingRadians()
        );
        setTurnRadarRightRadians(radarTurn * 2);

        // 修正后的预测瞄准算法
        double bulletPower = Math.min(3, 400 / e.getDistance());
        double bulletSpeed = 20 - 3 * bulletPower;
        double deltaTime = e.getDistance() / bulletSpeed;
        
        // 计算敌人当前位置
        double absoluteBearing = getHeadingRadians() + e.getBearingRadians();
        double enemyX = getX() + e.getDistance() * Math.sin(absoluteBearing);
        double enemyY = getY() + e.getDistance() * Math.cos(absoluteBearing);
        
        // 预测敌人未来位置
        double enemyVelocity = e.getVelocity();
        double enemyHeading = e.getHeadingRadians();
        double predictedX = enemyX + Math.sin(enemyHeading) * enemyVelocity * deltaTime;
        double predictedY = enemyY + Math.cos(enemyHeading) * enemyVelocity * deltaTime;

        // 计算瞄准角度
        double targetAngle = Math.atan2(
            predictedX - getX(), 
            predictedY - getY()
        );
        
        double gunTurn = Utils.normalRelativeAngle(
            targetAngle - getGunHeadingRadians()
        );
        setTurnGunRightRadians(gunTurn);

        if (getGunHeat() == 0 && Math.abs(gunTurn) < Math.PI/30) {
            fire(bulletPower);
            lastScanTime = getTime();
        }
    }

    public void onRobotDeath(RobotDeathEvent e) {
        if (e.getName().equals(lockedEnemy)) {
            lockedEnemy = null;
            enemyDistance = 0;
            setTurnRadarRightRadians(Double.POSITIVE_INFINITY);
        }
    }

    public void onHitByBullet(HitByBulletEvent e) {
        isEvading = true;
        
        setMaxVelocity(10);
        setTurnRightRadians(Math.PI - rand.nextDouble() * Math.PI/2);
        setBack(200);
        
        new Thread(() -> {
            try {
                Thread.sleep(300);
                isEvading = false;
                setMaxVelocity(8);
            } catch (Exception ex) {}
        }).start();
    }

    public void onHitWall(HitWallEvent e) {
        setBack(200);
        setTurnRight(135 + rand.nextInt(90));
        execute();
        direction *= -1;
    }

    public void onHitRobot(HitRobotEvent e) {
        if (e.isMyFault()) {
            setBack(150);
            setTurnRight(90 + rand.nextInt(90));
        } else {
            setTurnRight(e.getBearing() + 90);
            setAhead(150);
        }
        
        if (e.getName().equals(lockedEnemy)) {
            fire(3);
        }
    }
}
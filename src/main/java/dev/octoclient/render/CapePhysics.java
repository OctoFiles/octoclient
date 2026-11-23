package dev.octoclient.render;

/**
 * Direct Java port of tr7zw's WaveyCapes simulation (WCVersionless). Point 0 is
 * the part pinned to the player; the remaining points keep their previous
 * positions, so the cloth swings, overshoots and settles instead of following
 * the body rigidly. The three variants are the ones WaveyCapes offers through
 * its "Cape Movement" setting.
 *
 * <p>Coordinates are the ones WaveyCapes uses: +y up, the cape hangs towards
 * -y, and x < 0 means "behind" (the first point is the reference). The caller
 * translates them into body space when building the mesh.</p>
 */
final class CapePhysics {
    private CapePhysics() {
    }

    /** The four Cape Movement variants, in the same order as WaveyCapes. */
    enum Mode {
        CLASSIC, SIMULATION, SIMULATION_V2, DUNGEONS
    }

    static Mode mode(String value) {
        return switch (value) {
            case "Simulation" -> Mode.SIMULATION;
            case "Simulation V2" -> Mode.SIMULATION_V2;
            case "Dungeons" -> Mode.DUNGEONS;
            default -> Mode.CLASSIC;
        };
    }

    /** Creates the simulation matching a Cape Movement value. */
    static Simulation create(String movement, int partCount) {
        Simulation simulation = switch (mode(movement)) {
            case SIMULATION -> new StickSimulation();
            case SIMULATION_V2 -> new StickSimulation3d();
            case DUNGEONS -> new StickSimulationDungeons();
            default -> null;
        };
        if (simulation != null) {
            simulation.init(partCount);
        }
        return simulation;
    }

    /** BasicSimulation from WaveyCapes, with the point views flattened to arrays. */
    abstract static class Simulation {
        float[] x = new float[0];
        float[] y = new float[0];
        float[] z = new float[0];
        private float[] prevX = new float[0];
        private float[] prevY = new float[0];
        private float[] prevZ = new float[0];
        private final int numIterations;

        float gravityX;
        float gravityY = -1f;
        float gravityZ;
        float gravity;
        boolean sneaking;

        private float dirX;
        private float dirY;
        private float dirZ;

        Simulation(int numIterations) {
            this.numIterations = numIterations;
        }

        boolean init(int partCount) {
            boolean resized = x.length != partCount;
            if (resized) {
                x = new float[partCount];
                y = new float[partCount];
                z = new float[partCount];
                prevX = new float[partCount];
                prevY = new float[partCount];
                prevZ = new float[partCount];
            }
            for (int i = 0; i < partCount; i++) {
                y[i] = -i;
            }
            return resized;
        }

        boolean empty() {
            return x.length < 2;
        }

        void setGravityDirection(float gx, float gy, float gz) {
            gravityX = gx;
            gravityY = gy;
            gravityZ = gz;
        }

        void move(float dx, float dy, float dz) {
            prevX[0] = x[0];
            prevY[0] = y[0];
            prevZ[0] = z[0];
            x[0] += dx;
            y[0] += dy;
            z[0] += dz;
        }

        void applyGravity() {
            float fall = gravity * (50f / 1000f); // fixed timescale, as in WaveyCapes
            for (int i = 1; i < x.length; i++) {
                prevX[i] = x[i];
                prevY[i] = y[i];
                prevZ[i] = z[i];
                x[i] += gravityX * fall;
                y[i] += gravityY * fall;
                z[i] += gravityZ * fall;
            }
        }

        void preventClipping() {
            for (int i = 1; i < x.length; i++) {
                if (x[i] - x[0] > 0) {
                    x[i] = x[0];
                }
                float t = (float) i / x.length;
                float maxZ = t * t * 5f;
                float offset = z[0] - z[i];
                if (offset > maxZ) {
                    z[i] = z[0] - maxZ;
                }
                if (offset < -maxZ) {
                    z[i] = z[0] + maxZ;
                }
            }
        }

        void applyMotion() {
            for (int i = 0; i < numIterations; i++) {
                for (int a = x.length - 2; a >= 0; a--) {
                    direction(a, a + 1);
                    separate(a, a + 1);
                }
            }
        }

        void limitLength() {
            for (int a = 0; a < x.length - 1; a++) {
                direction(a, a + 1);
                x[a + 1] = x[a] - dirX;
                y[a + 1] = y[a] - dirY;
                z[a + 1] = z[a] - dirZ;
            }
        }

        void preventSelfClipping(int maxRuns) {
            boolean clipped;
            int runs = 0;
            do {
                clipped = false;
                for (int a = 0; a < x.length; a++) {
                    for (int b = a + 1; b < x.length; b++) {
                        float dx = x[a] - x[b];
                        float dy = y[a] - y[b];
                        float dz = z[a] - z[b];
                        if (dx * dx + dy * dy + dz * dz < 0.99f) {
                            clipped = true;
                            runs++;
                            direction(a, b);
                            separate(a, b);
                        }
                    }
                }
            } while (clipped && runs < maxRuns);
        }

        void preventHardBend(int i, float maxBend) {
            float abx = x[i - 1] - x[i];
            float aby = y[i - 1] - y[i];
            float cbx = x[i - 1] - x[i + 1];
            float cby = y[i - 1] - y[i + 1];
            float dot = abx * cbx + aby * cby;
            float cross = abx * cby - aby * cbx;
            double angle = Math.atan2(cross, dot) * 180d / Math.PI;
            if (angle < -maxBend) {
                swing(i, -maxBend * 2);
            }
            if (angle > maxBend) {
                swing(i, maxBend * 2);
            }
        }

        private void swing(int i, float deg) {
            float ox = x[i] - x[i - 1];
            float oy = y[i] - y[i - 1];
            float oz = z[i] - z[i - 1];
            double rad = Math.toRadians(deg);
            float cos = (float) Math.cos(rad);
            float sin = (float) Math.sin(rad);
            x[i + 1] = cos * ox - sin * oy + x[i];
            y[i + 1] = sin * ox + cos * oy + y[i];
            z[i + 1] = oz + z[i];
        }

        private void separate(int a, int b) {
            float cx = (x[a] + x[b]) / 2f;
            float cy = (y[a] + y[b]) / 2f;
            float cz = (z[a] + z[b]) / 2f;
            if (a != 0) {
                x[a] = cx + dirX / 2f;
                y[a] = cy + dirY / 2f;
                z[a] = cz + dirZ / 2f;
            }
            x[b] = cx - dirX / 2f;
            y[b] = cy - dirY / 2f;
            z[b] = cz - dirZ / 2f;
        }

        private void direction(int a, int b) {
            dirX = x[a] - x[b];
            dirY = y[a] - y[b];
            dirZ = z[a] - z[b];
            float length = (float) Math.sqrt(dirX * dirX + dirY * dirY + dirZ * dirZ);
            if (length < 1.0e-4f) {
                dirX = 0f;
                dirY = 0f;
                dirZ = 0f;
            } else {
                dirX /= length;
                dirY /= length;
                dirZ /= length;
            }
        }

        float lerpX(int index, float delta) {
            return prevX[index] + delta * (x[index] - prevX[index]);
        }

        float lerpY(int index, float delta) {
            return prevY[index] + delta * (y[index] - prevY[index]);
        }

        float lerpZ(int index, float delta) {
            return prevZ[index] + delta * (z[index] - prevZ[index]);
        }

        abstract void simulate();
    }

    /** WaveyCapes StickSimulation (2D, MAX_BEND 5). */
    static final class StickSimulation extends Simulation {
        StickSimulation() {
            super(3);
        }

        @Override
        void setGravityDirection(float gx, float gy, float gz) {
            gravityX = gx;
            gravityY = gy;
        }

        void applyMovement(float dx, float dy, float dz) {
            move(dx, dy, 0f);
        }

        @Override
        void simulate() {
            applyGravity();
            preventClipping();
            preventHardBends();
            applyMotion();
            limitLength();
        }

        private void preventHardBends() {
            for (int i = x.length - 2; i >= 1; i--) {
                double angle = Math.atan2(y[i + 1] - y[i], x[i + 1] - x[i])
                        - Math.atan2(y[i - 1] - y[i], x[i - 1] - x[i]);
                angle *= 57.2958d;
                if (angle > 360d) {
                    angle -= 360d;
                }
                if (angle < -360d) {
                    angle += 360d;
                }
                double abs = Math.abs(angle);
                if (abs < 180d - 5d) {
                    replace(i, angle, 180d - 5d + 1d);
                }
                if (abs > 180d + 5d) {
                    replace(i, angle, 180d + 5d - 1d);
                }
            }
        }

        private void replace(int i, double angle, double target) {
            double theta = target / 57.2958d;
            float px = x[i - 1] - x[i];
            float py = y[i - 1] - y[i];
            if (angle < 0) {
                theta *= -1;
            }
            float cos = (float) Math.cos(theta);
            float sin = (float) Math.sin(theta);
            x[i + 1] = px * cos - py * sin + x[i];
            y[i + 1] = px * sin + py * cos + y[i];
        }
    }

    /** WaveyCapes StickSimulation3d (MAX_BEND 20, self-clipping prevention). */
    static final class StickSimulation3d extends Simulation {
        private static final float MAX_BEND = 20f;

        StickSimulation3d() {
            super(3);
        }

        @Override
        boolean init(int partCount) {
            boolean resized = super.init(partCount);
            for (int i = 0; i < partCount; i++) {
                x[i] = -i;
            }
            return resized;
        }

        @Override
        void setGravityDirection(float gx, float gy, float gz) {
            gravityX = gx;
            gravityY = gy;
            gravityZ = gz;
        }

        void applyMovement(float dx, float dy, float dz) {
            move(dx, dy, dz);
        }

        @Override
        void simulate() {
            applyGravity();
            preventClipping();
            preventSelfClipping(32);
            applyMotion();
            preventSelfClipping(32);
            for (int i = 1; i < x.length - 2; i++) {
                preventHardBend(i, MAX_BEND);
            }
            limitLength();
        }
    }

    /** WaveyCapes StickSimulationDungeons (30 iterations, MAX_BEND at the middle). */
    static final class StickSimulationDungeons extends Simulation {
        StickSimulationDungeons() {
            super(30);
        }

        @Override
        void setGravityDirection(float gx, float gy, float gz) {
            gravityX = gx;
            gravityY = gy;
            gravityZ = gz;
        }

        void applyMovement(float dx, float dy, float dz) {
            move(dx, dy, dz);
        }

        @Override
        void simulate() {
            applyGravity();
            preventClipping();
            applyMotion();
            for (int i = 1; i < x.length - 1; i++) {
                preventHardBend(i, i == x.length / 2 ? 20f : 0f);
            }
            preventSelfClipping(10);
            limitLength();
        }
    }
}

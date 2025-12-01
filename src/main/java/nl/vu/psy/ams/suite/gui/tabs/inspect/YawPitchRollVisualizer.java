package nl.vu.psy.ams.suite.gui.tabs.inspect;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.util.Arrays;
import javax.swing.JPanel;
import nl.vu.psy.ams.suite.gui.drawing.BinaryDoubleDataDrawer;
import nl.vu.psy.ams.suite.gui.drawing.OnlineDrawer;

public class YawPitchRollVisualizer extends JPanel {

    @SuppressWarnings("unused")
    private final BinaryDoubleDataDrawer ya, p, r;
    @SuppressWarnings("unused")
    private OnlineDrawer ya_online, p_online, r_online;

    // Cube vertices (in 3D space) with updated dimensions (2x2x1)
    private final double[][] cubeVertices = {
            { -1, -1, -0.5 }, { 1, -1, -0.5 }, { 1, 1, -0.5 }, { -1, 1, -0.5 }, // Front face
            { -1, -1, 0.5 }, { 1, -1, 0.5 }, { 1, 1, 0.5 }, { -1, 1, 0.5 } // Back face
    };

    // Cube faces (defined as sets of vertices)
    private final int[][] cubeFaces = {
            { 0, 1, 2, 3 }, // Front face
            { 4, 5, 6, 7 }, // Back face
            { 0, 1, 5, 4 }, // Bottom face
            { 2, 3, 7, 6 }, // Top face
            { 1, 2, 6, 5 }, // Right face
            { 0, 3, 7, 4 } // Left face
    };

    // Colors for each face
    private final Color[] faceColors = {
            Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.CYAN, Color.MAGENTA
    };

    // Rotation angles
    private double yaw = 0; // Rotation around Z-axis (YAW)
    private double pitch = 90; // Rotation around X-axis (PITCH)
    private double roll = 0; // Rotation around Y-axis (ROLL)
    // Zoom level
    private final double zoom = 300;

    public YawPitchRollVisualizer(BinaryDoubleDataDrawer ya, BinaryDoubleDataDrawer p, BinaryDoubleDataDrawer r) {
        setBackground(Color.LIGHT_GRAY); // Set background to dark color
        this.ya = ya;
        this.r = r;
        this.p = p;
        this.ya_online = null;
        this.p_online = null;
        this.r_online = null;
    }

    public YawPitchRollVisualizer(OnlineDrawer ya, OnlineDrawer p, OnlineDrawer r) {
        setBackground(Color.LIGHT_GRAY); // Set background to dark color
        this.ya_online = ya;
        this.r_online = r;
        this.p_online = p;
        this.ya = null;
        this.p = null;
        this.r = null;
    }

    public void setYawDrawer(OnlineDrawer ya) {
        this.ya_online = ya;
    }

    public void setPitchDrawer(OnlineDrawer p) {
        this.p_online = p;
    }

    public void setRollDrawer(OnlineDrawer r) {
        this.r_online = r;
    }

    public void setRotation(double time) {
        yaw = 0;// ya.getValueAtTime(time);
        if (p != null)
            pitch = p.getValueAtTime(time);
        else
            pitch = p_online.getValueAtTime(time);
        roll = 0;// r.getValueAtTime(time);
        repaint();
    }

    public void setYaw(double yaw) {
        this.yaw = yaw;
    }

    public void setPitch(double pitch) {
        this.pitch = pitch;
    }

    public void setRoll(double roll) {
        this.roll = roll;
    }

    // Rotate points around the X-axis (pitch)
    private double[] rotateX(double x, double y, double z, double angle) {
        double rad = Math.toRadians(angle);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        return new double[] {
                x,
                cos * y - sin * z,
                sin * y + cos * z
        };
    }

    // Rotate points around the Y-axis (roll)
    private double[] rotateY(double x, double y, double z, double angle) {
        double rad = Math.toRadians(angle);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        return new double[] {
                cos * x + sin * z,
                y,
                -sin * x + cos * z
        };
    }

    // Rotate points around the Z-axis (yaw)
    private double[] rotateZ(double x, double y, double z, double angle) {
        double rad = Math.toRadians(angle);
        double cos = Math.cos(rad);
        double sin = Math.sin(rad);
        return new double[] {
                cos * x - sin * y,
                sin * x + cos * y,
                z
        };
    }

    // Project 3D points to 2D
    private Point project(double x, double y, double z) {
        double perspective = zoom / (z + 5);
        int projectedX = (int) (x * perspective + getWidth() / 2);
        int projectedY = (int) (y * perspective + getHeight() / 2);
        return new Point(projectedX, projectedY);
    }

    // Apply dynamic lighting to face color
    private Color applyLighting(Color baseColor, double[] normal, double[] lightDirection) {
        double dotProduct = Math.max(0,
                normal[0] * lightDirection[0] + normal[1] * lightDirection[1] + normal[2] * lightDirection[2]);
        float factor = (float) (0.5 + 0.5 * dotProduct); // Blend between ambient and diffuse
        return new Color(
                Math.min(255, (int) (baseColor.getRed() * factor)),
                Math.min(255, (int) (baseColor.getGreen() * factor)),
                Math.min(255, (int) (baseColor.getBlue() * factor)));
    }

    // Compute normal vector of a face
    private double[] computeNormal(double[][] vertices, int[] face) {
        double[] v1 = vertices[face[0]];
        double[] v2 = vertices[face[1]];
        double[] v3 = vertices[face[2]];
        double[] normal = {
                (v2[1] - v1[1]) * (v3[2] - v1[2]) - (v2[2] - v1[2]) * (v3[1] - v1[1]),
                (v2[2] - v1[2]) * (v3[0] - v1[0]) - (v2[0] - v1[0]) * (v3[2] - v1[2]),
                (v2[0] - v1[0]) * (v3[1] - v1[1]) - (v2[1] - v1[1]) * (v3[0] - v1[0])
        };
        double magnitude = Math.sqrt(normal[0] * normal[0] + normal[1] * normal[1] + normal[2] * normal[2]);
        return new double[] { normal[0] / magnitude, normal[1] / magnitude, normal[2] / magnitude };
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Apply rotations (yaw, pitch, roll)
        double[][] rotatedVertices = new double[cubeVertices.length][3];
        for (int i = 0; i < cubeVertices.length; i++) {
            double[] vertex = cubeVertices[i];
            vertex = rotateX(vertex[0], vertex[1], vertex[2], roll); // Apply roll (X-axis)
            vertex = rotateY(vertex[0], vertex[1], vertex[2], pitch); // Apply pitch (Y-axis)
            vertex = rotateZ(vertex[0], vertex[1], vertex[2], yaw); // Apply yaw (Z-axis)

            rotatedVertices[i] = vertex;
        }

        // Compute depth for each face
        double[] faceDepths = new double[cubeFaces.length];
        for (int i = 0; i < cubeFaces.length; i++) {
            int[] face = cubeFaces[i];
            double depthSum = 0;
            for (int vertexIndex : face) {
                depthSum += rotatedVertices[vertexIndex][2]; // Use Z-coordinate
            }
            faceDepths[i] = depthSum / face.length; // Average depth
        }

        // Sort faces by depth (farthest to nearest)
        Integer[] faceOrder = new Integer[cubeFaces.length];
        for (int i = 0; i < faceOrder.length; i++) {
            faceOrder[i] = i;
        }
        Arrays.sort(faceOrder, (a, b) -> Double.compare(faceDepths[b], faceDepths[a])); // Descending order

        // Light source direction
        double[] lightDirection = { 0, 0, -1 }; // Light coming from the viewer's direction

        // Draw faces in sorted order
        for (int faceIndex : faceOrder) {
            int[] face = cubeFaces[faceIndex];
            Path2D path = new Path2D.Double();
            for (int vertexIndex : face) {
                double[] vertex = rotatedVertices[vertexIndex];
                Point p = project(vertex[0], vertex[1], vertex[2]);
                if (path.getCurrentPoint() == null) {
                    path.moveTo(p.x, p.y);
                } else {
                    path.lineTo(p.x, p.y);
                }
            }
            path.closePath();

            // Apply lighting
            double[] normal = computeNormal(rotatedVertices, face);
            Color shadedColor = applyLighting(faceColors[faceIndex], normal, lightDirection);
            g2d.setColor(shadedColor);
            g2d.fill(path);

            // Draw edges
            g2d.setColor(Color.BLACK);
            g2d.setStroke(new BasicStroke(2));
            g2d.draw(path);

            // Add text to the red face (index 0)
            if (faceIndex == 0) { // Red face
                // Compute the center of the face in 3D space
                double centerX = 0, centerY = 0, centerZ = 0;
                for (int vertexIndex : face) {
                    double[] vertex = rotatedVertices[vertexIndex];
                    centerX += vertex[0];
                    centerY += vertex[1];
                    centerZ += vertex[2];
                }
                centerX /= face.length;
                centerY /= face.length;
                centerZ /= face.length;

                // Project the center point
                Point centerPoint = project(centerX, centerY, centerZ);

                // Set up text properties
                g2d.setColor(Color.BLACK);
                g2d.setFont(new Font("Arial", Font.BOLD, 16));
                String text = "BACK";

                // Get FontMetrics to calculate the width of the text
                FontMetrics fm = g2d.getFontMetrics();
                int textWidth = fm.stringWidth(text);
                int textHeight = fm.getAscent(); // Ascent gives the height from baseline to the top of the text

                // Apply the 3D transformation to the text's position
                // We will rotate the text in 2D according to the face's current orientation

                // Compute two edge vectors for orientation (to define text alignment)
                // double[] edge1 = {
                // rotatedVertices[face[1]][0] - rotatedVertices[face[0]][0],
                // rotatedVertices[face[1]][1] - rotatedVertices[face[0]][1],
                // rotatedVertices[face[1]][2] - rotatedVertices[face[0]][2],
                // };
                double[] edge2 = {
                        rotatedVertices[face[3]][0] - rotatedVertices[face[0]][0],
                        rotatedVertices[face[3]][1] - rotatedVertices[face[0]][1],
                        rotatedVertices[face[3]][2] - rotatedVertices[face[0]][2],
                };

                // Project the edge points to determine the correct angle of the text
                // Point edgePoint1 = project(centerX + edge1[0], centerY + edge1[1], centerZ +
                // edge1[2]);
                Point edgePoint2 = project(centerX + edge2[0], centerY + edge2[1], centerZ + edge2[2]);

                // Calculate angle of rotation for the text
                double angle = Math.atan2(edgePoint2.y - centerPoint.y, edgePoint2.x - centerPoint.x);

                // Now rotate the text based on the calculated angle
                AffineTransform originalTransform = g2d.getTransform();
                AffineTransform transform = new AffineTransform();
                transform.translate(centerPoint.x, centerPoint.y); // Move the origin to the center of the face
                transform.rotate(angle); // Rotate to align with the orientation of the face
                g2d.setTransform(transform);

                // Draw the text centered on the face
                g2d.drawString(text, -textWidth / 2, textHeight / 2);

                // Restore the original transform state
                g2d.setTransform(originalTransform);
            }

            // Add the white ball on the cyan face
            if (faceIndex == 4) { // Cyan face
                // Compute the center of the face
                double centerX = 0, centerY = 0, centerZ = 0;
                for (int vertexIndex : face) {
                    double[] vertex = rotatedVertices[vertexIndex];
                    centerX += vertex[0];
                    centerY += vertex[1];
                    centerZ += vertex[2];
                }
                centerX /= face.length;
                centerY /= face.length;
                centerZ /= face.length;

                // Project the center point
                Point centerPoint = project(centerX, centerY, centerZ);

                // Draw the white ball
                int ballRadius = 30;
                g2d.setColor(Color.WHITE);
                g2d.fillOval(centerPoint.x - ballRadius, centerPoint.y - ballRadius, ballRadius * 2, ballRadius * 2);
                g2d.setColor(Color.BLACK);
                g2d.drawOval(centerPoint.x - ballRadius, centerPoint.y - ballRadius, ballRadius * 2, ballRadius * 2);
            }
        }

        // Draw the 2D axes representation in the bottom-left corner
        // draw2DAxes(g2d);
    }

    // private void draw2DAxes(Graphics2D g2d) {
    // int axisLength = 50; // Length of the axes
    // int padding = 10; // Padding from the bottom-left corner
    // int originX = padding + axisLength; // Origin x-coordinate
    // int originY = getHeight() - padding - axisLength; // Origin y-coordinate

    // // Draw the axes lines
    // g2d.setColor(Color.BLACK);
    // g2d.setStroke(new BasicStroke(2));
    // g2d.drawLine(originX, originY, originX + axisLength, originY); // X-axis
    // g2d.drawLine(originX, originY, originX, originY - axisLength); // Y-axis

    // // Draw arrowheads for the X-axis
    // g2d.drawLine(originX + axisLength, originY, originX + axisLength - 5, originY
    // - 5);
    // g2d.drawLine(originX + axisLength, originY, originX + axisLength - 5, originY
    // + 5);

    // // Draw arrowheads for the Y-axis
    // g2d.drawLine(originX, originY - axisLength, originX - 5, originY - axisLength
    // + 5);
    // g2d.drawLine(originX, originY - axisLength, originX + 5, originY - axisLength
    // + 5);

    // // Label the axes
    // g2d.setFont(new Font("Arial", Font.BOLD, 12));
    // g2d.drawString("X", originX + axisLength + 5, originY + 5);
    // g2d.drawString("Y", originX - 10, originY - axisLength - 5);
    // }

    // public static void main(String[] args) {
    // JFrame frame = new JFrame("Yaw, Pitch, Roll Visualizer");
    // YawPitchRollVisualizer panel = new YawPitchRollVisualizer();
    // frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    // frame.setSize(800, 600);
    // frame.setContentPane(panel);
    // frame.setVisible(true);
    // }
}

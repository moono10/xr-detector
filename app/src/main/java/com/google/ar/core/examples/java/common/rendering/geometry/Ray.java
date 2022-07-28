package com.google.ar.core.examples.java.common.rendering.geometry;

public class Ray {
    public Vector3 origin;
    public Vector3 direction;

    public Ray(Vector3 origin, Vector3 direction) {
        this.origin = origin;
        this.direction = direction;
    }

    public Vector3[] getSkewPoints(Ray other) {
        Vector3 o1 = this.origin;
        Vector3 d1 = this.direction;
        Vector3 o2 = other.origin;
        Vector3 d2 = other.direction;

        //https://www.youtube.com/watch?v=R6SkZvBibO4

        float a = -(d1.x * d2.x + d1.y * d2.y + d1.z * d2.z);

        float b = -(d1.x * d1.x + d1.y * d1.y + d1.z * d1.z);

        float s =	(
                (  o1.x * d1.x - o2.x * d1.x
                        +  o1.y * d1.y - o2.y * d1.y
                        +  o1.z * d1.z - o2.z * d1.z )  *  a

                        - (  o1.x * d2.x - o2.x * d2.x
                        +  o1.y * d2.y - o2.y * d2.y
                        +  o1.z * d2.z - o2.z * d2.z  )  * b
        )
                /
                ( d2.x  * d1.x * a + d2.y * d1.y * a + d2.z * d1.z * a
                        - d2.x  * d2.x * b - d2.y * d2.y * b - d2.z * d2.z * b);

        float t =   (
                o1.x * d2.x - o2.x * d2.x - d2.x * s * d2.x
                        + o1.y * d2.y - o2.y * d2.y - d2.y * s * d2.y
                        + o1.z * d2.z - o2.z * d2.z - d2.z * s * d2.z)
                / a;


        if (Float.isInfinite(s) || Float.isInfinite(t)) {
            throw new RuntimeException("??????");
        }

        if (Float.isNaN(s) && Float.isNaN(t)) {
            throw new RuntimeException("이건 평행하다");
        }

        if (Float.isNaN(s)) {
            throw new RuntimeException("어떤 케이스지");
        }

        if (Float.isNaN(t)) {
            throw new RuntimeException("직교한거 같은데?");
        }

        Vector3 p1 = new Vector3(o1.x + d1.x * t, o1.y + d1.y * t, o1.z + d1.z * t);

        Vector3 p2 = new Vector3(o2.x + d2.x * s, o2.y + d2.y * s, o2.z + d2.z * s);

        return new Vector3[] {p1, p2};
    }
}

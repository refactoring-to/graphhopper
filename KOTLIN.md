

Analyse dependencies to pick a good place to start.

 - API is the leaf dependency.
 - Web-api is a root dependency.
 
Starting in API, the util.shapes are easy to convert.
 - BBox and Circle are value types.  But the auto-converted `equals` method is custom.  It needs a bit of cleanup when converted to kotlin.
    - note use of @JvmOverloads in Circle to allow the default arguments to apply when called from Java
 - GHPlace depends on GHPoint, so pick GHPoint first
 - GHPoint is *almost* a value object.  Can we turn it into one?  Let's lean on the compiler.


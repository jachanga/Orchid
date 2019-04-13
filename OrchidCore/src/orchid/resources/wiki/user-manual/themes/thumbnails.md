---
description: 'Orchid has support for basic thumbnail generation to keep your pages loading fast.'
sampleAsset: 'assets/media/pic09.jpg'
---

## Media Management

Orchid includes basic support for media management, including simple image manipulation. You can use the `asset()` 
template function to load an asset, and Orchid will make sure it ends up in your final site. 

## Basic Usage 

```jinja
{% verbatim %}
{{ 'assets/image.jpg'|asset }}
{% endverbatim %}
```

![asset]({{ sampleAsset|asset }})

## Thumbnail Generation 

**Rotate**

Rotate an image asset. Rotation angle is expressed in degrees.

```jinja
{% verbatim %}
{{ 'assets/image.jpg'|asset|rotate(90) }}
{% endverbatim %}
```

![rotated asset]({{ sampleAsset|asset|rotate(90) }})

{% docs className='com.eden.orchid.impl.themes.functions.RotateFunction' tableClass='table' tableLeaderClass='hidden' %}

**Scale**

Scale an image asset by a constant factor.

```jinja
{% verbatim %}
{{ 'assets/image.jpg'|asset|scale(0.85) }}
{% endverbatim %}
```

![scaled asset]({{ sampleAsset|asset|scale(0.85) }})

{% docs className='com.eden.orchid.impl.themes.functions.ScaleFunction' tableClass='table' tableLeaderClass='hidden' %}

**Resize**

Resize an image asset to specific dimensions. By default, image is resized maintaining its aspect ratio, and is reduced 
to the largest image that can fit entirely within the specified dimensions. Use the `exact=true` parameter to resize the
image to exactly the specified dimensions, stretching the image as necessary to fit. 

```jinja
{% verbatim %}
{{ 'assets/image.jpg'|asset|resize(800, 600, "exact") }}
{% endverbatim %}
```

![resized asset]({{ sampleAsset|asset|resize(400, 300, "fit") }})
![exact resized asset]({{ sampleAsset|asset|resize(400, 300, "exact") }})
![resized cropped center-left asset]({{ sampleAsset|asset|resize(400, 300, "cl") }})
![resized cropped center asset]({{ sampleAsset|asset|resize(400, 300, "c") }})
![resized cropped center-right asset]({{ sampleAsset|asset|resize(400, 300, "cr") }})

{% docs className='com.eden.orchid.impl.themes.functions.ResizeFunction' tableClass='table' tableLeaderClass='hidden' %}

**Chaining**

Multiple transformations may be applied to a single asset. Simply use more than one of the above filters. You can use 
the same filter more than once, and they will be applied in turn from left-to-right. 

```jinja
{% verbatim %}
{{ 'assets/image.jpg'|asset|resize(800, 600, exact=true)|rotate(45)|rotate(45) }}
{% endverbatim %}
```

![resized asset]({{ sampleAsset|asset|resize(400, 300) }})
![resized asset]({{ sampleAsset|asset|resize(400, 300)|rotate(45) }})
![resized asset]({{ sampleAsset|asset|resize(400, 300)|rotate(45)|rotate(45) }})

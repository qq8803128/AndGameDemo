package cn.ollyice.library.butterknife;

import android.view.View;
import cn.ollyice.library.butterknife.internal.ListenerClass;
import cn.ollyice.library.butterknife.internal.ListenerMethod;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static android.view.View.OnLongClickListener;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.CLASS;

/**
 * Bind a method to an {@link OnLongClickListener OnLongClickListener} on the view for each ID
 * specified.
 * <pre><code>
 * {@literal @}OnLongClick(R.id.example) boolean onLongClick() {
 *   Toast.makeText(this, "Long clicked!", Toast.LENGTH_SHORT).show();
 *   return true;
 * }
 * </code></pre>
 * Any number of parameters from {@link OnLongClickListener#onLongClick(android.view.View)} may be
 * used on the method.
 *
 * @see OnLongClickListener
 */
@Retention(CLASS) @Target(METHOD)
@ListenerClass(
    targetType = "android.view.View",
    setter = "setOnLongClickListener",
    type = "android.view.View.OnLongClickListener",
    method = @ListenerMethod(
        name = "onLongClick",
        parameters = {
            "android.view.View"
        },
        returnType = "boolean",
        defaultReturn = "false"
    )
)
public @interface OnLongClick {
  /** View IDs to which the method will be bound. */
  String[] value();
}

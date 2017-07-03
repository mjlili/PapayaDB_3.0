package fr.umlv.papayaDB.apiClient;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Scanner;

import fr.umlv.papayaDB.apiClient.ApiClient.DatabaseQuery;

public class MainClient {

	public static ClassValue<Method[]> CLASS_VALUE = new ClassValue<Method[]>() {
		@Override
		protected Method[] computeValue(Class<?> type) {
			return type.getMethods();
		}
	};

	private static String call(Method method, Object receiver, Object[] args) {
		try {
			if (args.length > 1) {
				return (String) method.invoke(receiver, args[0], args[1]);
			}
			return (String) method.invoke(receiver, args[0]);

		} catch (IllegalAccessException e) {
			throw new IllegalStateException("should not happen");
		} catch (InvocationTargetException e) {
			Throwable t = e.getCause();
			if (t instanceof RuntimeException) {
				throw (RuntimeException) t;
			}
			if (t instanceof Error) {
				throw (Error) t;
			}
			throw new UndeclaredThrowableException(t);
		}
	}

	public static void main(String[] args) {

		try (Scanner sc = new Scanner(System.in)) {
			ApiClient httpClient;
			try {
				httpClient = new ApiClient();
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException(
						"The REST server address " + ApiClient.SERVER_ADDRESS + "is not valid.");
			}

			while (true) {
				System.out.println("Enter your query :");
				String userQuery = sc.nextLine();
				if (userQuery.equals("exit")) {
					break;
				}
				try {
					String[] command = userQuery.split("\\s->\\s", 2);
					if (command.length > 1) {
						String[] arguments = command[1].split("\\s");
						Arrays.stream(CLASS_VALUE.get(httpClient.getClass()))
								.filter(method -> method.isAnnotationPresent(DatabaseQuery.class))
								.filter(method -> method.getAnnotation(DatabaseQuery.class).value()
										.equalsIgnoreCase(command[0]))
								.forEach(method -> System.out.println(call(method, httpClient, arguments)));
					} else if (command[0].equalsIgnoreCase("GET ALL DATABASES")) {
						System.out.println(httpClient.getAllDatabases());
					} else {
						System.out.println("request wrong unrecognize, please respect the syntax.");
					}
				} catch (IllegalArgumentException | IndexOutOfBoundsException e) {
					e.printStackTrace();
					System.out.println("Should not Happen ");
				}
			}
		}
	}
}
